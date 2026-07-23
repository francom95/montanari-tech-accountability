package com.montanaritech.contable.impuestos.atribucion;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.CalcularRequest;
import com.montanaritech.contable.impuestos.atribucion.dto.AtribucionImpuestoDtos.PorcentajeProyecto;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibb;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIva;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atribución de impuestos liquidados a proyectos (F6.3). Resuelve el monto a
 * repartir (el saldo a pagar de la liquidación), calcula los pesos por proyecto
 * según el criterio, y reparte con {@link ProrrateoCalculator} (residuo en la
 * última línea). Persiste la atribución para que F7.4 la consuma sin recalcular.
 */
@Service
@RequiredArgsConstructor
public class AtribucionImpuestoService {

    private static final String MONEDA_IMPUESTOS = "ARS";

    private final AtribucionImpuestoRepository repo;
    private final ConfiguracionAtribucionRepository configRepo;
    private final LiquidacionIvaRepository liquidacionIvaRepository;
    private final LiquidacionIibbRepository liquidacionIibbRepository;
    private final FacturaVentaRepository facturaVentaRepository;
    private final FacturaCompraRepository facturaCompraRepository;
    private final ProyectoRepository proyectoRepository;
    private final MonedaRepository monedaRepository;
    private final AtribucionImpuestoMapper mapper;
    private final AuditoriaService auditoria;

    // --- lectura ---

    @Transactional(readOnly = true)
    public CriterioAtribucion criterioPorDefecto() {
        return configRepo.findFirstByOrderByIdAsc()
                .map(ConfiguracionAtribucion::getCriterioPorDefecto)
                .orElse(CriterioAtribucion.FACTURACION);
    }

    /** La atribución guardada de una liquidación; si no hay, una previsualización con el criterio por defecto. */
    @Transactional(readOnly = true)
    public CalculoAtribucion obtener(TipoLiquidacion tipo, Long liquidacionId) {
        return repo.findByLiquidacionTipoAndLiquidacionId(tipo, liquidacionId)
                .map(this::aCalculo)
                .orElseGet(() -> calcular(tipo, liquidacionId,
                        new CalcularRequest(criterioPorDefecto(), null, null), false));
    }

    @Transactional(readOnly = true)
    public CalculoAtribucion previsualizar(TipoLiquidacion tipo, Long liquidacionId, CalcularRequest req) {
        return calcular(tipo, liquidacionId, req, false);
    }

    // --- escritura ---

    @Transactional
    public CalculoAtribucion guardar(TipoLiquidacion tipo, Long liquidacionId, CalcularRequest req) {
        LiquidacionRef ref = resolverLiquidacion(tipo, liquidacionId);
        CalculoAtribucion calculo = calcular(tipo, liquidacionId, req, true);

        AtribucionImpuesto a = repo.findByLiquidacionTipoAndLiquidacionId(tipo, liquidacionId)
                .orElseGet(AtribucionImpuesto::new);
        boolean nueva = a.getId() == null;
        var antes = nueva ? null : mapper.aResponse(aCalculo(a));

        a.setLiquidacionTipo(tipo);
        a.setLiquidacionId(liquidacionId);
        a.setAnio(ref.anio());
        a.setMes(ref.mes());
        a.setCriterio(req.criterio());
        a.setMoneda(monedaArs());
        a.setTipoCambio(BigDecimal.ONE);
        a.setMontoTotal(ref.montoTotal());
        a.getLineas().clear();

        int orden = 1;
        for (CalculoAtribucion.LineaCalculada lc : calculo.lineas()) {
            AtribucionImpuestoLinea linea = new AtribucionImpuestoLinea();
            linea.setAtribucionImpuesto(a);
            linea.setProyecto(proyectoRepository.getReferenceById(lc.proyectoId()));
            linea.setPorcentaje(lc.porcentaje());
            linea.setMonto(lc.monto());
            linea.setOrden(orden++);
            a.getLineas().add(linea);
        }

        AtribucionImpuesto guardada = repo.save(a);
        auditoria.registrar(nueva ? AccionAuditoria.CREAR : AccionAuditoria.EDITAR,
                "AtribucionImpuesto", guardada.getId(), antes, mapper.aResponse(aCalculo(guardada)));
        return aCalculo(guardada);
    }

    @Transactional
    public CriterioAtribucion actualizarCriterioPorDefecto(CriterioAtribucion criterio) {
        ConfiguracionAtribucion cfg = configRepo.findFirstByOrderByIdAsc()
                .orElseGet(ConfiguracionAtribucion::new);
        var antes = cfg.getCriterioPorDefecto();
        cfg.setCriterioPorDefecto(criterio);
        configRepo.save(cfg);
        auditoria.registrar(AccionAuditoria.EDITAR, "ConfiguracionAtribucion", cfg.getId(),
                antes != null ? antes.name() : null, criterio.name());
        return criterio;
    }

    // --- cálculo ---

    private CalculoAtribucion calcular(TipoLiquidacion tipo, Long liquidacionId, CalcularRequest req, boolean guardada) {
        LiquidacionRef ref = resolverLiquidacion(tipo, liquidacionId);
        List<String> advertencias = new ArrayList<>();

        List<ProrrateoCalculator.Peso> pesos = switch (req.criterio()) {
            case DIRECTO -> pesosDirecto(req);
            case FACTURACION -> pesosPorFacturacion(ref, advertencias);
            case MARGEN -> pesosPorMargen(ref, advertencias);
            case PORCENTAJE_MANUAL -> pesosManuales(req);
        };

        List<CalculoAtribucion.LineaCalculada> lineas = new ArrayList<>();
        if (!pesos.isEmpty()) {
            for (ProrrateoCalculator.Reparto r : ProrrateoCalculator.repartir(ref.montoTotal(), pesos)) {
                Proyecto p = proyectoRepository.findById(r.proyectoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + r.proyectoId() + " no encontrado"));
                lineas.add(new CalculoAtribucion.LineaCalculada(p.getId(), p.getNombre(), r.porcentaje(), r.monto()));
            }
        } else {
            advertencias.add("No hay proyectos con base para repartir el impuesto en este período con el criterio elegido.");
        }

        return new CalculoAtribucion(tipo, liquidacionId, ref.anio(), ref.mes(), req.criterio(),
                ref.montoTotal(), guardada, lineas, advertencias);
    }

    private List<ProrrateoCalculator.Peso> pesosDirecto(CalcularRequest req) {
        if (req.proyectoIdDirecto() == null) {
            throw new NegocioException("ATRIBUCION_DIRECTO_SIN_PROYECTO",
                    "La atribución directa necesita el proyecto al que se imputa el 100%");
        }
        proyectoRepository.findById(req.proyectoIdDirecto())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + req.proyectoIdDirecto() + " no encontrado"));
        return List.of(new ProrrateoCalculator.Peso(req.proyectoIdDirecto(), BigDecimal.ONE));
    }

    private List<ProrrateoCalculator.Peso> pesosManuales(CalcularRequest req) {
        if (req.porcentajes() == null || req.porcentajes().isEmpty()) {
            throw new NegocioException("ATRIBUCION_MANUAL_SIN_PORCENTAJES",
                    "La atribución por porcentaje manual necesita al menos un proyecto con su porcentaje");
        }
        BigDecimal suma = req.porcentajes().stream()
                .map(PorcentajeProyecto::porcentaje)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (suma.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new NegocioException("ATRIBUCION_MANUAL_NO_SUMA_100",
                    "Los porcentajes deben sumar 100 (suman %s)".formatted(suma.toPlainString()));
        }
        return req.porcentajes().stream()
                .filter(p -> p.porcentaje().signum() > 0)
                .sorted(Comparator.comparing(PorcentajeProyecto::proyectoId))
                .map(p -> new ProrrateoCalculator.Peso(p.proyectoId(), p.porcentaje()))
                .toList();
    }

    private List<ProrrateoCalculator.Peso> pesosPorFacturacion(LiquidacionRef ref, List<String> advertencias) {
        Map<Long, BigDecimal> ventas = ventasPorProyecto(ref, advertencias);
        return pesosPositivos(ventas);
    }

    private List<ProrrateoCalculator.Peso> pesosPorMargen(LiquidacionRef ref, List<String> advertencias) {
        Map<Long, BigDecimal> ventas = ventasPorProyecto(ref, advertencias);
        Map<Long, BigDecimal> compras = comprasPorProyecto(ref);
        Map<Long, BigDecimal> margen = new LinkedHashMap<>(ventas);
        compras.forEach((proy, monto) -> margen.merge(proy, monto.negate(), BigDecimal::add));
        List<ProrrateoCalculator.Peso> pesos = pesosPositivos(margen);
        if (pesos.isEmpty() && !margen.isEmpty()) {
            advertencias.add("Ningún proyecto tiene margen positivo en el período; no se puede repartir por margen.");
        }
        return pesos;
    }

    /** Ventas netas por proyecto en el período (notas de crédito restan). Facturas sin proyecto se avisan. */
    private Map<Long, BigDecimal> ventasPorProyecto(LiquidacionRef ref, List<String> advertencias) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        BigDecimal sinProyecto = BigDecimal.ZERO;
        for (FacturaVenta f : facturaVentaRepository.buscarConfirmadasParaReporte(
                null, null, null, ref.fechaDesde(), ref.fechaHasta())) {
            BigDecimal neto = f.getNetoGravado();
            if (f.getTipoComprobante().name().startsWith("NOTA_CREDITO")) {
                neto = neto.negate();
            }
            if (f.getProyecto() == null) {
                sinProyecto = sinProyecto.add(neto);
            } else {
                map.merge(f.getProyecto().getId(), neto, BigDecimal::add);
            }
        }
        if (sinProyecto.signum() != 0 && advertencias != null) {
            advertencias.add(("Hay ventas por %s sin proyecto asignado: no entran en el reparto por facturación/margen.")
                    .formatted(sinProyecto.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }
        return map;
    }

    private Map<Long, BigDecimal> comprasPorProyecto(LiquidacionRef ref) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        for (FacturaCompra f : facturaCompraRepository.buscarConfirmadasParaReporte(
                null, null, null, ref.fechaDesde(), ref.fechaHasta())) {
            BigDecimal neto = f.getNeto();
            if (f.getTipoComprobante().name().startsWith("NOTA_CREDITO")) {
                neto = neto.negate();
            }
            if (f.getProyecto() != null) {
                map.merge(f.getProyecto().getId(), neto, BigDecimal::add);
            }
        }
        return map;
    }

    private List<ProrrateoCalculator.Peso> pesosPositivos(Map<Long, BigDecimal> pesosPorProyecto) {
        return pesosPorProyecto.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ProrrateoCalculator.Peso(e.getKey(), e.getValue()))
                .toList();
    }

    // --- resolución de la liquidación (polimórfica) ---

    private record LiquidacionRef(int anio, int mes, LocalDate fechaDesde, LocalDate fechaHasta, BigDecimal montoTotal) {
    }

    private LiquidacionRef resolverLiquidacion(TipoLiquidacion tipo, Long liquidacionId) {
        return switch (tipo) {
            case IVA -> {
                LiquidacionIva l = liquidacionIvaRepository.findById(liquidacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Liquidación de IVA " + liquidacionId + " no encontrada"));
                exigirConfirmada(l.getEstado());
                yield new LiquidacionRef(l.getAnio(), l.getMes(), l.getFechaDesde(), l.getFechaHasta(), l.getSaldoAPagar());
            }
            case IIBB -> {
                LiquidacionIibb l = liquidacionIibbRepository.findById(liquidacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Liquidación de IIBB " + liquidacionId + " no encontrada"));
                exigirConfirmada(l.getEstado());
                yield new LiquidacionRef(l.getAnio(), l.getMes(), l.getFechaDesde(), l.getFechaHasta(), l.getSaldoAPagarTotal());
            }
        };
    }

    private void exigirConfirmada(EstadoDocumento estado) {
        if (estado != EstadoDocumento.CONFIRMADO) {
            throw new NegocioException("LIQUIDACION_NO_CONFIRMADA",
                    "Solo se puede atribuir a proyectos una liquidación confirmada (esta está %s)".formatted(estado));
        }
    }

    private Moneda monedaArs() {
        return monedaRepository.findByCodigo(MONEDA_IMPUESTOS)
                .orElseThrow(() -> new NegocioException("MONEDA_ARS_FALTANTE",
                        "No está configurada la moneda " + MONEDA_IMPUESTOS));
    }

    private CalculoAtribucion aCalculo(AtribucionImpuesto a) {
        List<CalculoAtribucion.LineaCalculada> lineas = a.getLineas().stream()
                .map(l -> new CalculoAtribucion.LineaCalculada(
                        l.getProyecto().getId(), l.getProyecto().getNombre(), l.getPorcentaje(), l.getMonto()))
                .toList();
        return new CalculoAtribucion(a.getLiquidacionTipo(), a.getLiquidacionId(), a.getAnio(), a.getMes(),
                a.getCriterio(), a.getMontoTotal(), true, lineas, List.of());
    }
}
