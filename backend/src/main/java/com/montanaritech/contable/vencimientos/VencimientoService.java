package com.montanaritech.contable.vencimientos;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.dashboard.ConfiguracionDashboard;
import com.montanaritech.contable.dashboard.ConfiguracionDashboardRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibb;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIva;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.concepto.Concepto;
import com.montanaritech.contable.maestros.concepto.ConceptoRepository;
import com.montanaritech.contable.maestros.concepto.Periodicidad;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import com.montanaritech.contable.vencimientos.dto.GenerarAutomaticosResponse;
import com.montanaritech.contable.vencimientos.dto.GenerarAutomaticosResponse.GeneradoPorTipo;
import com.montanaritech.contable.vencimientos.dto.VencimientoCrearRequest;
import com.montanaritech.contable.vencimientos.dto.VencimientoEditarRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de {@link Vencimiento} (F8.1), mismo molde que {@code ClienteService}/{@code ConceptoService}. */
@Service
@RequiredArgsConstructor
public class VencimientoService {

    private final VencimientoRepository repo;
    private final MonedaRepository monedaRepository;
    private final CuentaContableRepository cuentaContableRepository;
    private final ProveedorRepository proveedorRepository;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final ProyectoRepository proyectoRepository;
    private final ConceptoRepository conceptoRepository;
    private final AsientoRepository asientoRepository;
    private final LiquidacionIvaRepository liquidacionIvaRepository;
    private final LiquidacionIibbRepository liquidacionIibbRepository;
    private final ConfiguracionDashboardRepository configuracionDashboardRepository;
    private final VencimientoMapper mapper;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<Vencimiento> listar(TipoVencimiento tipo, EstadoVencimientoObligacion estado, LocalDate fechaDesde,
            LocalDate fechaHasta, Long proyectoId, Long proveedorId, Long tarjetaId, Pageable p) {
        return repo.buscar(tipo, estado, fechaDesde, fechaHasta, proyectoId, proveedorId, tarjetaId, p);
    }

    /**
     * "Próximos vencimientos" (F9.1/F8.3): pendientes con fecha hasta el
     * límite de la ventana, incluye los ya vencidos (fecha pasada). Método
     * público reusable directamente por otros servicios, sin pasar por HTTP.
     */
    @Transactional(readOnly = true)
    public List<Vencimiento> proximos(int dias) {
        LocalDate limite = LocalDate.now().plusDays(dias);
        return repo.findByEstadoAndFechaLessThanEqualOrderByFechaAsc(EstadoVencimientoObligacion.PENDIENTE, limite);
    }

    @Transactional(readOnly = true)
    public Vencimiento obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vencimiento " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Vencimiento")
    @Transactional
    public Vencimiento crear(VencimientoCrearRequest req) {
        Vencimiento v = new Vencimiento();
        aplicar(v, req.descripcion(), req.tipo(), req.fecha(), req.importeEstimado(), req.monedaId(),
                req.recurrencia(), req.intervaloDiasPersonalizado(), req.cuentaContableId(), req.proveedorId(),
                req.liquidacionTipo(), req.liquidacionId(), req.tarjetaCreditoId(), req.proyectoId(),
                req.conceptoRecurrenteId(), req.observaciones());
        return repo.save(v);
    }

    @Transactional
    public Vencimiento editar(Long id, VencimientoEditarRequest req) {
        Vencimiento v = obtener(id);
        validarEditable(v);
        var antes = mapper.aResponse(v);
        aplicar(v, req.descripcion(), req.tipo(), req.fecha(), req.importeEstimado(), req.monedaId(),
                req.recurrencia(), req.intervaloDiasPersonalizado(), req.cuentaContableId(), req.proveedorId(),
                req.liquidacionTipo(), req.liquidacionId(), req.tarjetaCreditoId(), req.proyectoId(),
                req.conceptoRecurrenteId(), req.observaciones());
        auditoria.registrar(AccionAuditoria.EDITAR, "Vencimiento", id, antes, mapper.aResponse(v));
        return v;
    }

    private void validarEditable(Vencimiento v) {
        if (v.getEstado() != EstadoVencimientoObligacion.PENDIENTE
                && v.getEstado() != EstadoVencimientoObligacion.REPROGRAMADO) {
            throw new IllegalStateException(
                    "Solo se puede editar un vencimiento PENDIENTE o REPROGRAMADO (estado actual: " + v.getEstado() + ")");
        }
    }

    private void aplicar(Vencimiento v, String descripcion, TipoVencimiento tipo, LocalDate fecha,
            java.math.BigDecimal importeEstimado, Long monedaId, TipoRecurrencia recurrencia,
            Integer intervaloDiasPersonalizado, Long cuentaContableId, Long proveedorId,
            com.montanaritech.contable.impuestos.atribucion.TipoLiquidacion liquidacionTipo, Long liquidacionId,
            Long tarjetaCreditoId, Long proyectoId, Long conceptoRecurrenteId, String observaciones) {
        v.setDescripcion(descripcion);
        v.setTipo(tipo);
        v.setFecha(fecha);
        v.setImporteEstimado(importeEstimado);
        v.setMoneda(resolverMoneda(monedaId));
        v.setRecurrencia(recurrencia);
        v.setIntervaloDiasPersonalizado(intervaloDiasPersonalizado);
        v.setCuentaContable(resolverCuentaContable(cuentaContableId));
        v.setProveedor(resolverProveedor(proveedorId));
        v.setLiquidacionTipo(liquidacionTipo);
        v.setLiquidacionId(liquidacionId);
        v.setTarjetaCredito(resolverTarjetaCredito(tarjetaCreditoId));
        v.setProyecto(resolverProyecto(proyectoId));
        v.setConceptoRecurrente(resolverConcepto(conceptoRecurrenteId));
        v.setObservaciones(observaciones);
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    private CuentaContable resolverCuentaContable(Long id) {
        if (id == null) {
            return null;
        }
        return cuentaContableRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }

    private Proveedor resolverProveedor(Long id) {
        if (id == null) {
            return null;
        }
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    private TarjetaCredito resolverTarjetaCredito(Long id) {
        if (id == null) {
            return null;
        }
        return tarjetaCreditoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarjeta de crédito " + id + " no encontrada"));
    }

    private Proyecto resolverProyecto(Long id) {
        if (id == null) {
            return null;
        }
        return proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Concepto resolverConcepto(Long id) {
        if (id == null) {
            return null;
        }
        return conceptoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Concepto " + id + " no encontrado"));
    }

    @Transactional
    public Vencimiento marcarPagado(Long id, Long asientoId) {
        Vencimiento v = obtener(id);
        if (v.getEstado() == EstadoVencimientoObligacion.PAGADO || v.getEstado() == EstadoVencimientoObligacion.CANCELADO) {
            throw new IllegalStateException("El vencimiento ya está " + v.getEstado());
        }
        var antes = mapper.aResponse(v);
        v.setEstado(EstadoVencimientoObligacion.PAGADO);
        if (asientoId != null) {
            Asiento asiento = asientoRepository.findById(asientoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Asiento " + asientoId + " no encontrado"));
            v.setAsientoVinculado(asiento);
        }
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Vencimiento", id, antes, mapper.aResponse(v));
        return v;
    }

    @Transactional
    public Vencimiento reprogramar(Long id, LocalDate nuevaFecha, String motivo) {
        Vencimiento v = obtener(id);
        if (v.getEstado() == EstadoVencimientoObligacion.PAGADO || v.getEstado() == EstadoVencimientoObligacion.CANCELADO) {
            throw new IllegalStateException("El vencimiento ya está " + v.getEstado());
        }
        var antes = mapper.aResponse(v);
        v.setFecha(nuevaFecha);
        v.setEstado(EstadoVencimientoObligacion.REPROGRAMADO);
        if (motivo != null) {
            v.setObservaciones(motivo);
        }
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Vencimiento", id, antes, mapper.aResponse(v));
        return v;
    }

    @Transactional
    public Vencimiento cancelar(Long id, String motivo) {
        Vencimiento v = obtener(id);
        if (v.getEstado() == EstadoVencimientoObligacion.PAGADO || v.getEstado() == EstadoVencimientoObligacion.CANCELADO) {
            throw new IllegalStateException("El vencimiento ya está " + v.getEstado());
        }
        var antes = mapper.aResponse(v);
        v.setEstado(EstadoVencimientoObligacion.CANCELADO);
        v.setMotivoCancelacion(motivo);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Vencimiento", id, antes, mapper.aResponse(v));
        return v;
    }

    /**
     * Motor de generación automática (F8.1), disparado on-demand desde el
     * frontend — el proyecto no tiene scheduling y este paso no lo introduce.
     * Cada fuente es idempotente: correr esto dos veces seguidas no duplica
     * nada (ver {@link VencimientoRepository}, métodos {@code existsBy...}).
     * Todas las fuentes son solo lectura sobre servicios ya cerrados
     * (liquidaciones, tarjetas, conceptos) — ninguno se modifica.
     */
    @Transactional
    public GenerarAutomaticosResponse generarAutomaticos() {
        List<GeneradoPorTipo> generados = new ArrayList<>();
        generados.add(new GeneradoPorTipo("LIQUIDACION_IVA", generarDesdeLiquidacionesIva()));
        generados.add(new GeneradoPorTipo("LIQUIDACION_IIBB", generarDesdeLiquidacionesIibb()));
        generados.add(new GeneradoPorTipo("TARJETA", generarDesdeTarjetas()));
        generados.add(new GeneradoPorTipo("CONCEPTO_RECURRENTE", generarDesdeConceptos()));
        generados.add(new GeneradoPorTipo("MANUAL", generarDesdeManualesRecurrentes()));
        int total = generados.stream().mapToInt(GeneradoPorTipo::cantidad).sum();
        return new GenerarAutomaticosResponse(generados, total);
    }

    private ConfiguracionDashboard configuracionDashboard() {
        return configuracionDashboardRepository.findFirstByOrderByIdAsc().orElseGet(ConfiguracionDashboard::new);
    }

    private Moneda monedaArs() {
        return monedaRepository.findByCodigo("ARS")
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda ARS no encontrada"));
    }

    /** Mismo cálculo que {@code DashboardService.vencimientoIva/vencimientoIibb}: día fijo del mes siguiente al período. */
    private LocalDate vencimientoDelPeriodo(int anio, int mes, int diaVencimiento) {
        return LocalDate.of(anio, mes, 1).plusMonths(1).withDayOfMonth(diaVencimiento);
    }

    private int generarDesdeLiquidacionesIva() {
        ConfiguracionDashboard config = configuracionDashboard();
        Moneda ars = monedaArs();
        int generados = 0;
        for (LiquidacionIva liq : liquidacionIvaRepository.findByEstado(EstadoDocumento.CONFIRMADO)) {
            if (repo.existsByOrigenGeneracionAndOrigenGeneracionRefId(OrigenGeneracionVencimiento.LIQUIDACION_IVA, liq.getId())) {
                continue;
            }
            Vencimiento v = new Vencimiento();
            v.setDescripcion("IVA " + liq.getMes() + "/" + liq.getAnio());
            v.setTipo(TipoVencimiento.IVA);
            v.setFecha(vencimientoDelPeriodo(liq.getAnio(), liq.getMes(), config.getDiaVencimientoIva()));
            v.setImporteEstimado(liq.getSaldoAPagar());
            v.setMoneda(ars);
            v.setOrigenGeneracion(OrigenGeneracionVencimiento.LIQUIDACION_IVA);
            v.setOrigenGeneracionRefId(liq.getId());
            repo.save(v);
            generados++;
        }
        return generados;
    }

    private int generarDesdeLiquidacionesIibb() {
        ConfiguracionDashboard config = configuracionDashboard();
        Moneda ars = monedaArs();
        int generados = 0;
        for (LiquidacionIibb liq : liquidacionIibbRepository.findByEstado(EstadoDocumento.CONFIRMADO)) {
            if (repo.existsByOrigenGeneracionAndOrigenGeneracionRefId(OrigenGeneracionVencimiento.LIQUIDACION_IIBB, liq.getId())) {
                continue;
            }
            Vencimiento v = new Vencimiento();
            v.setDescripcion("IIBB " + liq.getMes() + "/" + liq.getAnio());
            v.setTipo(TipoVencimiento.IIBB);
            v.setFecha(vencimientoDelPeriodo(liq.getAnio(), liq.getMes(), config.getDiaVencimientoIibb()));
            v.setImporteEstimado(liq.getSaldoAPagarTotal());
            v.setMoneda(ars);
            v.setOrigenGeneracion(OrigenGeneracionVencimiento.LIQUIDACION_IIBB);
            v.setOrigenGeneracionRefId(liq.getId());
            repo.save(v);
            generados++;
        }
        return generados;
    }

    /** Próxima ocurrencia del día {@code diaVencimiento} a partir de hoy (mes actual si no pasó, si no el siguiente). */
    private LocalDate proximaOcurrenciaTarjeta(int diaVencimiento) {
        LocalDate hoy = LocalDate.now();
        LocalDate esteMes = hoy.withDayOfMonth(Math.min(diaVencimiento, YearMonth.from(hoy).lengthOfMonth()));
        if (!esteMes.isBefore(hoy)) {
            return esteMes;
        }
        YearMonth proximoMes = YearMonth.from(hoy).plusMonths(1);
        return proximoMes.atDay(Math.min(diaVencimiento, proximoMes.lengthOfMonth()));
    }

    private int generarDesdeTarjetas() {
        Moneda ars = monedaArs();
        int generados = 0;
        for (TarjetaCredito tarjeta : tarjetaCreditoRepository.findByActivoTrue()) {
            LocalDate fecha = proximaOcurrenciaTarjeta(tarjeta.getDiaVencimiento());
            if (repo.existsByOrigenGeneracionAndOrigenGeneracionRefIdAndFecha(
                    OrigenGeneracionVencimiento.TARJETA, tarjeta.getId(), fecha)) {
                continue;
            }
            Vencimiento v = new Vencimiento();
            v.setDescripcion("Tarjeta " + tarjeta.getEntidad());
            v.setTipo(TipoVencimiento.TARJETA);
            v.setFecha(fecha);
            v.setImporteEstimado(tarjeta.getSaldoActual());
            v.setMoneda(ars);
            v.setTarjetaCredito(tarjeta);
            v.setOrigenGeneracion(OrigenGeneracionVencimiento.TARJETA);
            v.setOrigenGeneracionRefId(tarjeta.getId());
            repo.save(v);
            generados++;
        }
        return generados;
    }

    private int generarDesdeConceptos() {
        int generados = 0;
        for (Concepto concepto : conceptoRepository.findByActivoTrue()) {
            if (concepto.getPeriodicidad() == Periodicidad.UNICA) {
                continue;
            }
            LocalDate creacion = concepto.getCreadoEn().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate desde;
            LocalDate hasta;
            LocalDate fecha;
            if (concepto.getPeriodicidad() == Periodicidad.MENSUAL) {
                YearMonth mesActual = YearMonth.now();
                desde = mesActual.atDay(1);
                hasta = mesActual.atEndOfMonth();
                fecha = mesActual.atDay(Math.min(creacion.getDayOfMonth(), mesActual.lengthOfMonth()));
            } else {
                int anioActual = LocalDate.now().getYear();
                desde = LocalDate.of(anioActual, 1, 1);
                hasta = LocalDate.of(anioActual, 12, 31);
                YearMonth mesDeCreacion = YearMonth.of(anioActual, creacion.getMonthValue());
                fecha = mesDeCreacion.atDay(Math.min(creacion.getDayOfMonth(), mesDeCreacion.lengthOfMonth()));
            }
            if (repo.existsByOrigenGeneracionAndOrigenGeneracionRefIdAndFechaBetween(
                    OrigenGeneracionVencimiento.CONCEPTO_RECURRENTE, concepto.getId(), desde, hasta)) {
                continue;
            }
            Vencimiento v = new Vencimiento();
            v.setDescripcion(concepto.getNombre());
            v.setTipo(TipoVencimiento.OTRO);
            v.setFecha(fecha);
            v.setImporteEstimado(concepto.getImporte());
            v.setMoneda(concepto.getMoneda() != null ? concepto.getMoneda() : monedaArs());
            v.setConceptoRecurrente(concepto);
            v.setOrigenGeneracion(OrigenGeneracionVencimiento.CONCEPTO_RECURRENTE);
            v.setOrigenGeneracionRefId(concepto.getId());
            repo.save(v);
            generados++;
        }
        return generados;
    }

    /** +1 mes / +1 año / +N días según la recurrencia, para encadenar la próxima ocurrencia manual. */
    private LocalDate proximaOcurrenciaManual(Vencimiento resuelto) {
        return switch (resuelto.getRecurrencia()) {
            case MENSUAL -> resuelto.getFecha().plusMonths(1);
            case ANUAL -> resuelto.getFecha().plusYears(1);
            case PERSONALIZADA -> resuelto.getFecha().plusDays(
                    resuelto.getIntervaloDiasPersonalizado() == null ? 30 : resuelto.getIntervaloDiasPersonalizado());
            case UNICA -> throw new IllegalStateException("UNICA no debería llegar acá (ver query findByOrigenGeneracionAndRecurrenciaNotAndEstadoIn)");
        };
    }

    /**
     * Vencimientos manuales recurrentes ya resueltos (PAGADO/CANCELADO): encadena la
     * próxima ocurrencia. {@code origenGeneracionRefId} del nuevo apunta al resuelto
     * que lo originó, para que una segunda corrida no lo vuelva a generar.
     */
    private int generarDesdeManualesRecurrentes() {
        int generados = 0;
        List<Vencimiento> resueltos = repo.findByOrigenGeneracionAndRecurrenciaNotAndEstadoIn(
                OrigenGeneracionVencimiento.MANUAL, TipoRecurrencia.UNICA,
                List.of(EstadoVencimientoObligacion.PAGADO, EstadoVencimientoObligacion.CANCELADO));
        for (Vencimiento resuelto : resueltos) {
            LocalDate proxima = proximaOcurrenciaManual(resuelto);
            if (repo.existsByOrigenGeneracionAndOrigenGeneracionRefIdAndFecha(
                    OrigenGeneracionVencimiento.MANUAL, resuelto.getId(), proxima)) {
                continue;
            }
            Vencimiento v = new Vencimiento();
            v.setDescripcion(resuelto.getDescripcion());
            v.setTipo(resuelto.getTipo());
            v.setFecha(proxima);
            v.setImporteEstimado(resuelto.getImporteEstimado());
            v.setMoneda(resuelto.getMoneda());
            v.setRecurrencia(resuelto.getRecurrencia());
            v.setIntervaloDiasPersonalizado(resuelto.getIntervaloDiasPersonalizado());
            v.setCuentaContable(resuelto.getCuentaContable());
            v.setProveedor(resuelto.getProveedor());
            v.setProyecto(resuelto.getProyecto());
            v.setOrigenGeneracion(OrigenGeneracionVencimiento.MANUAL);
            v.setOrigenGeneracionRefId(resuelto.getId());
            repo.save(v);
            generados++;
        }
        return generados;
    }
}
