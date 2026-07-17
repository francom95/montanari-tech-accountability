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
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarRequest;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de asientos manuales (F3.4, sobre el diseño de F3.1 §3-§4). Único
 * punto de escritura de {@code asiento}/{@code asiento_linea} para carga
 * manual (ADR-07 de F3.1); los generadores automáticos de F4.x son un paso
 * futuro que reutiliza el mismo {@link ValidadorBalanceAsiento} y
 * {@link NumeradorAsiento}, no este service.
 *
 * <p><b>Alcance de este paso</b> (no todo lo que describe F3.1 §3-§4 es
 * F3.4): crear/editar/eliminar borrador y confirmar. La búsqueda avanzada,
 * duplicación, edición de confirmados y anulación son F3.5; los mayores y
 * saldos con su advertencia de "saldo contrario al esperado" son F3.6;
 * los generadores automáticos (factura/cobro/pago con diferencia de
 * cambio) son F4.x; el {@code PeriodoGuard} de escritura sobre período
 * cerrado es F9.3 (todavía no existe la entidad {@code Periodo}, así que el
 * ítem 5 del checklist de F3.1 §3.4 no aplica todavía — no hay período que
 * pueda estar cerrado).
 */
@Service
@RequiredArgsConstructor
public class AsientoService {

    private static final String MONEDA_LIBRO = "ARS";

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
    public Page<Asiento> listar(String texto, EstadoDocumento estado, Pageable p) {
        return repo.buscar(texto, estado, p);
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

        a.setNumero(numerador.siguienteNumero());
        a.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "Asiento", id, antes, mapper.aResponse(a));
        return a;
    }

    private Asiento obtenerBorrador(Long id) {
        Asiento a = obtener(id);
        if (a.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("TRANSICION_ESTADO_INVALIDA", "Solo se pueden editar o eliminar asientos en borrador");
        }
        return a;
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
            l.setCuentaContable(resolverCuenta(r.cuentaContableId()));
            l.setDebe(r.debe());
            l.setHaber(r.haber());
            l.setMoneda(resolverMoneda(r.monedaId()));
            l.setTipoCambio(r.tipoCambio());
            l.setImporteOriginal(r.importeOriginal());
            l.setLeyenda(r.leyenda());
            l.setProyecto(r.proyectoId() != null ? resolverProyecto(r.proyectoId()) : null);
            l.setEtapa(r.etapaId() != null ? resolverEtapa(r.etapaId()) : null);
            l.setCliente(r.clienteId() != null ? resolverCliente(r.clienteId()) : null);
            l.setProveedor(r.proveedorId() != null ? resolverProveedor(r.proveedorId()) : null);
            l.setCuentaBancaria(r.cuentaBancariaId() != null ? resolverCuentaBancaria(r.cuentaBancariaId()) : null);
            l.setGeneradaAuto(false);
            asiento.getLineas().add(l);
        }
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
