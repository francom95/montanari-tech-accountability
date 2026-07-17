package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.asiento.NumeradorAsiento;
import com.montanaritech.contable.common.asiento.ValidadorBalanceAsiento;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.estado.TransicionEstadoValidator;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarConfirmadoRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoLineaEditarRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoLineaRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.tipocambio.TipoCambio;
import com.montanaritech.contable.maestros.tipocambio.TipoCambioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de asientos, carga manual y ciclo de vida completo (F3.4/F3.5,
 * sobre el diseño de F3.1 §3-§4). Único punto de escritura de
 * {@code asiento}/{@code asiento_linea} (ADR-07 de F3.1); los generadores
 * automáticos de F4.x son un paso futuro que reutiliza el mismo
 * {@link ValidadorBalanceAsiento} y {@link NumeradorAsiento}, no este
 * service.
 *
 * <p><b>Alcance de F3.5</b> sobre lo que dejó F3.4: búsqueda avanzada,
 * duplicación, edición de confirmados (con la restricción de F3.1 §4.2 de
 * que solo ADMIN toca líneas {@code generada_auto = true} — hoy no las
 * produce nadie todavía, pero la regla ya es correcta y testeable) y
 * anulación por marca. <b>Deliberadamente fuera de alcance</b> (F9.3,
 * todavía no existe la entidad {@code Periodo}): el contra-asiento de
 * F3.1 §4.4 para fechas en período cerrado — sin esa entidad no hay forma
 * de determinar "período cerrado", así que hoy toda anulación es por
 * marca (el único camino posible mientras ningún período pueda estarlo);
 * el admin-override reforzado sobre período cerrado tampoco aplica
 * todavía por la misma razón. Los mayores/saldos con la advertencia de
 * "saldo contrario al esperado" son F3.6.
 */
@Service
@RequiredArgsConstructor
public class AsientoService {

    private static final String MONEDA_LIBRO = "ARS";

    /** F3.1 §4.4 D-3: estos orígenes se anulan desde su documento, no acá. */
    private static final Set<OrigenAsiento> ORIGENES_ANULABLES_DIRECTO = Set.of(
            OrigenAsiento.MANUAL, OrigenAsiento.AJUSTE, OrigenAsiento.APERTURA, OrigenAsiento.IMPORTACION);

    private final AsientoRepository repo;
    private final AsientoMapper mapper;
    private final AuditoriaService auditoria;
    private final NumeradorAsiento numerador;
    private final CuentaContableRepository cuentaRepo;
    private final MonedaRepository monedaRepo;
    private final ProyectoRepository proyectoRepo;
    private final EtapaRepository etapaRepo;
    private final ClienteRepository clienteRepo;
    private final ProveedorRepository proveedorRepo;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final TipoCambioRepository tipoCambioRepo;

    @Transactional(readOnly = true)
    public Page<Asiento> listar(
            String texto, EstadoDocumento estado, OrigenAsiento origen, Long numero,
            LocalDate fechaDesde, LocalDate fechaHasta, Long cuentaContableId, BigDecimal importe,
            Long proyectoId, Long clienteId, Long proveedorId, Pageable p) {
        return repo.buscar(texto, estado, origen, numero, fechaDesde, fechaHasta, cuentaContableId, importe,
                proyectoId, clienteId, proveedorId, p);
    }

    @Transactional(readOnly = true)
    public Asiento obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Asiento " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Asiento")
    @Transactional
    public Asiento crearBorrador(AsientoCrearRequest req) {
        Asiento a = new Asiento();
        a.setFecha(req.fecha());
        a.setDescripcion(req.descripcion());
        a.setObservaciones(req.observaciones());
        a.setEstado(EstadoDocumento.BORRADOR);
        a.setOrigen(OrigenAsiento.MANUAL);
        reemplazarLineas(a, req.lineas());
        return repo.save(a);
    }

    @Transactional
    public Asiento editarBorrador(Long id, AsientoEditarRequest req) {
        Asiento a = obtenerBorrador(id);
        var antes = mapper.aResponse(a);

        a.setFecha(req.fecha());
        a.setDescripcion(req.descripcion());
        a.setObservaciones(req.observaciones());
        reemplazarLineas(a, req.lineas());

        auditoria.registrar(AccionAuditoria.EDITAR, "Asiento", id, antes, mapper.aResponse(a));
        return a;
    }

    @Transactional
    public void eliminarBorrador(Long id) {
        Asiento a = obtenerBorrador(id);
        var antes = mapper.aResponse(a);
        repo.delete(a);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Asiento", id, antes, null);
    }

    /**
     * Checklist de confirmación (F3.1 §3.4, ítems 1-4 y 6; el ítem 5 —
     * PeriodoGuard— no aplica todavía, ver Javadoc de la clase). Todo en una
     * sola transacción: si cualquier validación falla, nada se persiste
     * (ni el número de secuencia, que solo se pide al final).
     */
    @Transactional
    public Asiento confirmar(Long id) {
        Asiento a = obtener(id);
        var antes = mapper.aResponse(a);

        TransicionEstadoValidator.validar(a.getEstado(), EstadoDocumento.CONFIRMADO);
        validarChecklistDeAsiento(a);

        a.setNumero(numerador.siguienteNumero());
        a.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "Asiento", id, antes, mapper.aResponse(a));
        return a;
    }

    /**
     * Edita un asiento ya {@code CONFIRMADO} (F3.1 §4.2): el asiento sigue
     * impactando reportes, así que el rebalanceo re-corre el mismo checklist
     * de {@link #confirmar}, sin renumerar ni cambiar de estado. Líneas
     * {@code generada_auto = true}: cualquier usuario puede dejarlas
     * intactas; solo ADMIN puede quitarlas o modificarlas (F3.1 §4.6).
     */
    @Transactional
    public Asiento editarConfirmado(Long id, AsientoEditarConfirmadoRequest req) {
        Asiento a = obtenerConfirmado(id);
        var antes = mapper.aResponse(a);
        boolean esAdmin = esAdmin();

        Map<Long, AsientoLinea> existentesPorId = a.getLineas().stream()
                .collect(Collectors.toMap(EntidadNegocio::getId, l -> l));
        Set<Long> idsEnRequest = req.lineas().stream()
                .map(AsientoLineaEditarRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean tocoLineaAutomatica = false;
        for (AsientoLinea existente : a.getLineas()) {
            if (existente.isGeneradaAuto() && !idsEnRequest.contains(existente.getId())) {
                if (!esAdmin) {
                    throw new NegocioException("SOLO_ADMIN_EDITA_LINEAS_AUTOMATICAS",
                            "Solo un administrador puede quitar líneas generadas automáticamente");
                }
                tocoLineaAutomatica = true;
            }
        }

        List<AsientoLinea> nuevasLineas = new ArrayList<>();
        int orden = 1;
        for (AsientoLineaEditarRequest r : req.lineas()) {
            AsientoLinea linea = r.id() != null ? existentesPorId.get(r.id()) : null;
            if (linea != null && linea.isGeneradaAuto()) {
                if (!lineaIgual(linea, r)) {
                    if (!esAdmin) {
                        throw new NegocioException("SOLO_ADMIN_EDITA_LINEAS_AUTOMATICAS",
                                "Solo un administrador puede modificar líneas generadas automáticamente");
                    }
                    aplicarDatosLinea(linea, r);
                    tocoLineaAutomatica = true;
                }
            } else if (linea != null) {
                aplicarDatosLinea(linea, r);
            } else {
                linea = new AsientoLinea();
                linea.setAsiento(a);
                linea.setGeneradaAuto(false);
                aplicarDatosLinea(linea, r);
            }
            linea.setOrden(orden++);
            nuevasLineas.add(linea);
        }

        a.setFecha(req.fecha());
        a.setDescripcion(req.descripcion());
        a.setObservaciones(req.observaciones());
        a.getLineas().clear();
        a.getLineas().addAll(nuevasLineas);

        validarChecklistDeAsiento(a);

        auditoria.registrar(AccionAuditoria.EDITAR, "Asiento", id, antes, mapper.aResponse(a),
                false, tocoLineaAutomatica ? "edición de líneas autogeneradas" : null);
        return a;
    }

    /**
     * Duplica un asiento en cualquier estado (F3.1 §4.3): siempre crea un
     * BORRADOR nuevo, sin número, con fecha de hoy y origen {@code MANUAL}
     * — aunque la fuente sea un asiento automático, el duplicado es una
     * operación nueva del usuario, así que sus líneas también quedan
     * {@code generada_auto = false}.
     */
    @Transactional
    public Asiento duplicar(Long id) {
        Asiento fuente = obtener(id);

        Asiento nuevo = new Asiento();
        nuevo.setFecha(LocalDate.now());
        nuevo.setDescripcion(fuente.getDescripcion());
        nuevo.setObservaciones(fuente.getObservaciones());
        nuevo.setEstado(EstadoDocumento.BORRADOR);
        nuevo.setOrigen(OrigenAsiento.MANUAL);

        int orden = 1;
        for (AsientoLinea l : fuente.getLineas()) {
            AsientoLinea copia = new AsientoLinea();
            copia.setAsiento(nuevo);
            copia.setOrden(orden++);
            copia.setCuentaContable(l.getCuentaContable());
            copia.setDebe(l.getDebe());
            copia.setHaber(l.getHaber());
            copia.setMoneda(l.getMoneda());
            copia.setTipoCambio(l.getTipoCambio());
            copia.setImporteOriginal(l.getImporteOriginal());
            copia.setFuenteTc(l.getFuenteTc());
            copia.setLeyenda(l.getLeyenda());
            copia.setProyecto(l.getProyecto());
            copia.setEtapa(l.getEtapa());
            copia.setCliente(l.getCliente());
            copia.setProveedor(l.getProveedor());
            copia.setCuentaBancaria(l.getCuentaBancaria());
            copia.setGeneradaAuto(false);
            nuevo.getLineas().add(copia);
        }

        Asiento guardado = repo.save(nuevo);
        auditoria.registrar(AccionAuditoria.DUPLICAR, "Asiento", fuente.getId(), null, null,
                false, "Duplicado como asiento id=" + guardado.getId());
        return guardado;
    }

    /**
     * Anula por marca (F3.1 §4.4): el único camino posible hoy, ver Javadoc
     * de la clase. Rechaza los orígenes de documento (D-3: se anulan desde
     * el comprobante, no directo) para evitar que el asiento y el
     * comprobante diverjan.
     */
    @Transactional
    public Asiento anular(Long id, String motivo) {
        Asiento a = obtener(id);
        if (!ORIGENES_ANULABLES_DIRECTO.contains(a.getOrigen())) {
            throw new NegocioException("ANULACION_VIA_DOCUMENTO",
                    "Este asiento fue generado por un comprobante: anulalo desde ahí, no directamente");
        }
        var antes = mapper.aResponse(a);
        TransicionEstadoValidator.validar(a.getEstado(), EstadoDocumento.ANULADO);

        a.setEstado(EstadoDocumento.ANULADO);
        a.setMotivoAnulacion(motivo);

        auditoria.registrar(AccionAuditoria.ANULAR, "Asiento", id, antes, mapper.aResponse(a));
        return a;
    }

    private Asiento obtenerBorrador(Long id) {
        Asiento a = obtener(id);
        if (a.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("TRANSICION_ESTADO_INVALIDA", "Solo se pueden editar o eliminar asientos en borrador");
        }
        return a;
    }

    private Asiento obtenerConfirmado(Long id) {
        Asiento a = obtener(id);
        if (a.getEstado() != EstadoDocumento.CONFIRMADO) {
            throw new NegocioException("ASIENTO_NO_CONFIRMADO", "Solo se pueden editar así los asientos confirmados");
        }
        return a;
    }

    private boolean esAdmin() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    /** Checklist de confirmación (F3.1 §3.4 ítems 2-4): 2..N líneas válidas y balance. */
    private void validarChecklistDeAsiento(Asiento a) {
        List<AsientoLinea> lineas = a.getLineas();
        if (lineas.isEmpty()) {
            throw new NegocioException("ASIENTO_SIN_LINEAS", "El asiento no tiene líneas");
        }
        if (lineas.size() < 2) {
            throw new NegocioException("ASIENTO_INCOMPLETO", "El asiento necesita al menos 2 líneas para confirmarse");
        }
        for (AsientoLinea linea : lineas) {
            validarYNormalizarLinea(linea, a.getFecha());
        }

        List<LineaAsientoGenerada> paraBalance = lineas.stream()
                .map(l -> new LineaAsientoGenerada(l.getCuentaContable().getCodigo(), l.getDebe(), l.getHaber(), l.getLeyenda()))
                .toList();
        ValidadorBalanceAsiento.validar(paraBalance);
    }

    /**
     * Compara una línea existente {@code generada_auto = true} contra el
     * pedido, para saber si el usuario intenta modificarla. En ARS,
     * {@code tipoCambio}/{@code importeOriginal} no se comparan: el cliente
     * nunca los reenvía (el service los recalcula siempre, §3.3), así que
     * no son "contenido editable" para esta detección.
     */
    private boolean lineaIgual(AsientoLinea existente, AsientoLineaEditarRequest r) {
        boolean esArs = MONEDA_LIBRO.equals(existente.getMoneda().getCodigo());
        return Objects.equals(existente.getCuentaContable().getId(), r.cuentaContableId())
                && existente.getDebe().compareTo(r.debe()) == 0
                && existente.getHaber().compareTo(r.haber()) == 0
                && Objects.equals(existente.getMoneda().getId(), r.monedaId())
                && (esArs || bigDecimalIguales(existente.getTipoCambio(), r.tipoCambio()))
                && (esArs || bigDecimalIguales(existente.getImporteOriginal(), r.importeOriginal()))
                && Objects.equals(existente.getLeyenda(), r.leyenda())
                && Objects.equals(idOrNull(existente.getProyecto()), r.proyectoId())
                && Objects.equals(idOrNull(existente.getEtapa()), r.etapaId())
                && Objects.equals(idOrNull(existente.getCliente()), r.clienteId())
                && Objects.equals(idOrNull(existente.getProveedor()), r.proveedorId())
                && Objects.equals(idOrNull(existente.getCuentaBancaria()), r.cuentaBancariaId());
    }

    private static boolean bigDecimalIguales(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private static Long idOrNull(EntidadNegocio e) {
        return e != null ? e.getId() : null;
    }

    /**
     * Valida la línea y, de paso, materializa las reglas de F3.1 §3.3 que son
     * una asignación más que una validación: en ARS, tipo de cambio 1 e
     * importe original = debe+haber; en moneda extranjera sin TC, intenta
     * resolverlo automáticamente (CP-19).
     */
    private void validarYNormalizarLinea(AsientoLinea linea, LocalDate fechaAsiento) {
        CuentaContable cuenta = linea.getCuentaContable();
        if (!cuenta.isImputable()) {
            throw new NegocioException("CUENTA_NO_IMPUTABLE",
                    "La cuenta %s (%s) es una cuenta madre: no puede recibir movimientos".formatted(cuenta.getCodigo(), cuenta.getNombre()));
        }
        if (!cuenta.isActivo()) {
            throw new NegocioException("CUENTA_INACTIVA", "La cuenta %s está inactiva".formatted(cuenta.getCodigo()));
        }

        boolean debeNoCero = linea.getDebe().compareTo(BigDecimal.ZERO) != 0;
        boolean haberNoCero = linea.getHaber().compareTo(BigDecimal.ZERO) != 0;
        if (debeNoCero == haberNoCero) {
            throw new NegocioException("LINEA_DEBE_XOR_HABER",
                    "La línea de %s debe tener importe en debe o en haber, no ambos ni ninguno".formatted(cuenta.getCodigo()));
        }
        BigDecimal montoLinea = debeNoCero ? linea.getDebe() : linea.getHaber();

        if (MONEDA_LIBRO.equals(linea.getMoneda().getCodigo())) {
            linea.setTipoCambio(new BigDecimal("1.000000"));
            linea.setImporteOriginal(montoLinea);
            linea.setFuenteTc(null);
            return;
        }

        if (linea.getImporteOriginal() == null) {
            throw new NegocioException("IMPORTE_ORIGINAL_REQUERIDO",
                    "La línea de %s está en moneda extranjera: falta el importe en moneda original".formatted(cuenta.getCodigo()));
        }

        BigDecimal tc = linea.getTipoCambio();
        AsientoLinea.FuenteTc fuente = AsientoLinea.FuenteTc.MANUAL;
        if (tc == null) {
            tc = resolverTipoCambioAutomatico(linea.getMoneda().getId(), fechaAsiento);
            fuente = AsientoLinea.FuenteTc.AUTOMATICO;
        }
        linea.setTipoCambio(tc);
        linea.setFuenteTc(fuente);

        BigDecimal arsCalculado = linea.getImporteOriginal().multiply(tc).setScale(2, RoundingMode.HALF_UP);
        if (arsCalculado.compareTo(montoLinea) != 0) {
            throw new NegocioException("MONTO_ARS_INCONSISTENTE",
                    "En la línea de %s, el importe (%s) no coincide con original × TC (%s)"
                            .formatted(cuenta.getCodigo(), montoLinea, arsCalculado));
        }
    }

    private BigDecimal resolverTipoCambioAutomatico(Long monedaId, LocalDate fecha) {
        return tipoCambioRepo.findFirstByMonedaIdAndFechaAndActivoTrueOrderByIdAsc(monedaId, fecha)
                .map(TipoCambio::getValorVenta)
                .orElseThrow(() -> new NegocioException("TC_FALTANTE",
                        "No hay tipo de cambio manual ni cotización cargada para esa moneda en la fecha del asiento"));
    }

    private void reemplazarLineas(Asiento asiento, List<AsientoLineaRequest> nuevas) {
        asiento.getLineas().clear();
        if (nuevas == null) {
            return;
        }
        int orden = 1;
        for (AsientoLineaRequest r : nuevas) {
            AsientoLinea l = new AsientoLinea();
            l.setAsiento(asiento);
            l.setOrden(orden++);
            aplicarDatosLinea(l, r.cuentaContableId(), r.debe(), r.haber(), r.monedaId(), r.tipoCambio(),
                    r.importeOriginal(), r.leyenda(), r.proyectoId(), r.etapaId(), r.clienteId(), r.proveedorId(),
                    r.cuentaBancariaId());
            l.setGeneradaAuto(false);
            asiento.getLineas().add(l);
        }
    }

    private void aplicarDatosLinea(AsientoLinea l, AsientoLineaEditarRequest r) {
        aplicarDatosLinea(l, r.cuentaContableId(), r.debe(), r.haber(), r.monedaId(), r.tipoCambio(),
                r.importeOriginal(), r.leyenda(), r.proyectoId(), r.etapaId(), r.clienteId(), r.proveedorId(),
                r.cuentaBancariaId());
    }

    private void aplicarDatosLinea(
            AsientoLinea l, Long cuentaContableId, BigDecimal debe, BigDecimal haber, Long monedaId,
            BigDecimal tipoCambio, BigDecimal importeOriginal, String leyenda, Long proyectoId, Long etapaId,
            Long clienteId, Long proveedorId, Long cuentaBancariaId) {
        l.setCuentaContable(resolverCuenta(cuentaContableId));
        l.setDebe(debe);
        l.setHaber(haber);
        l.setMoneda(resolverMoneda(monedaId));
        l.setTipoCambio(tipoCambio);
        l.setImporteOriginal(importeOriginal);
        l.setLeyenda(leyenda);
        l.setProyecto(proyectoId != null ? resolverProyecto(proyectoId) : null);
        l.setEtapa(etapaId != null ? resolverEtapa(etapaId) : null);
        l.setCliente(clienteId != null ? resolverCliente(clienteId) : null);
        l.setProveedor(proveedorId != null ? resolverProveedor(proveedorId) : null);
        l.setCuentaBancaria(cuentaBancariaId != null ? resolverCuentaBancaria(cuentaBancariaId) : null);
    }

    private CuentaContable resolverCuenta(Long id) {
        return cuentaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    private Proyecto resolverProyecto(Long id) {
        return proyectoRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Etapa resolverEtapa(Long id) {
        return etapaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Etapa " + id + " no encontrada"));
    }

    private Cliente resolverCliente(Long id) {
        return clienteRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente " + id + " no encontrado"));
    }

    private Proveedor resolverProveedor(Long id) {
        return proveedorRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    private CuentaBancaria resolverCuentaBancaria(Long id) {
        return cuentaBancariaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + id + " no encontrada"));
    }
}
