package com.montanaritech.contable.maestros.proyecto.rentabilidad;

import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.cobro.AplicacionAnticipoClienteRepository;
import com.montanaritech.contable.facturacion.cobro.CobroImputacionRepository;
import com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.facturacion.pago.AplicacionAnticipoProveedorRepository;
import com.montanaritech.contable.facturacion.pago.PagoImputacionRepository;
import com.montanaritech.contable.facturacion.pago.dto.ImputadoFacturaCompra;
import com.montanaritech.contable.impuestos.atribucion.AtribucionImpuestoLinea;
import com.montanaritech.contable.impuestos.atribucion.AtribucionImpuestoLineaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoCuota;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyecto;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoCalculado;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoProyecto;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoProyectoService;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.ComisionResumen;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.EtapaResumen;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.PresupuestoComparacion;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.ProveedorResumen;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.TotalPorMoneda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reporte de rentabilidad por proyecto (F7.4): agrega — sin recalcular — lo
 * que ya calculan F2.6 (presupuesto), F2.7 (comisiones), F4.4/F4.5
 * (facturación, cobros, pagos) y F6.3 (impuestos atribuidos). Mismo criterio
 * de agregación en bloque (sin N+1) que {@code CuentaPorCobrarService}.
 */
@Service
@RequiredArgsConstructor
public class ReporteRentabilidadProyectoService {

    private static final int ESCALA_INTERNA = 10;

    private final ProyectoRepository proyectoRepo;
    private final EtapaRepository etapaRepo;
    private final FacturaVentaRepository facturaVentaRepo;
    private final FacturaCompraRepository facturaCompraRepo;
    private final CobroImputacionRepository cobroImputacionRepo;
    private final AplicacionAnticipoClienteRepository aplicacionAnticipoClienteRepo;
    private final PagoImputacionRepository pagoImputacionRepo;
    private final AplicacionAnticipoProveedorRepository aplicacionAnticipoProveedorRepo;
    private final ComisionProyectoRepository comisionProyectoRepo;
    private final AtribucionImpuestoLineaRepository atribucionImpuestoLineaRepo;
    private final PresupuestoProyectoService presupuestoProyectoService;

    @Transactional(readOnly = true)
    public ReporteRentabilidadProyectoResponse obtener(Long proyectoId) {
        Proyecto proyecto = proyectoRepo.findById(proyectoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + proyectoId + " no encontrado"));

        List<String> advertencias = new ArrayList<>();

        List<Etapa> etapas = etapaRepo.findByProyectoIdOrderByFechaInicioAsc(proyectoId);
        List<EtapaResumen> etapasResumen = etapas.stream()
                .map(e -> new EtapaResumen(e.getId(), e.getNombre(), e.getEstado().name(), e.getPorcentajeAvance()))
                .toList();

        List<FacturaVenta> facturasVenta = facturaVentaRepo.buscarConfirmadasParaReporte(null, proyectoId, null, null, null);
        Ingresos ingresos = calcularIngresos(facturasVenta);

        List<FacturaCompra> facturasCompra = facturaCompraRepo.buscarConfirmadasParaReporte(null, proyectoId, null, null, null);
        Egresos egresos = calcularEgresos(facturasCompra);

        List<ComisionProyecto> comisiones = comisionProyectoRepo.findByProyectoIdAndActivoTrue(proyectoId);
        List<ComisionResumen> comisionesResumen = comisiones.stream()
                .map(c -> new ComisionResumen(c.getId(), c.getComisionista().getNombre(), c.getPorcentajeComision(),
                        c.getEstadoPago().name(), c.getImporteEstimado(), c.getImporteFinal(), c.getMoneda().getCodigo()))
                .toList();
        Map<Long, TotalPorMoneda> comisionesPorMoneda = new LinkedHashMap<>();
        for (ComisionProyecto c : comisiones) {
            BigDecimal importe = c.getImporteFinal() != null ? c.getImporteFinal() : c.getImporteEstimado();
            comisionesPorMoneda.merge(c.getMoneda().getId(),
                    new TotalPorMoneda(c.getMoneda().getId(), c.getMoneda().getCodigo(), importe),
                    (a, b) -> new TotalPorMoneda(a.monedaId(), a.monedaCodigo(), a.total().add(b.total())));
        }
        BigDecimal comisionesArs = comisionesPorMoneda.values().stream()
                .filter(t -> "ARS".equals(t.monedaCodigo()))
                .map(TotalPorMoneda::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hayComisionNoArs = comisionesPorMoneda.values().stream().anyMatch(t -> !"ARS".equals(t.monedaCodigo()));
        if (hayComisionNoArs) {
            advertencias.add("Hay comisiones en moneda distinta de ARS: no se incluyen en el margen real en pesos.");
        }

        List<AtribucionImpuestoLinea> atribuciones = atribucionImpuestoLineaRepo.findByProyectoId(proyectoId);
        BigDecimal impuestosAtribuidosArs = atribuciones.stream()
                .map(AtribucionImpuestoLinea::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PresupuestoComparacion presupuestoComparacion = calcularComparacionPresupuesto(proyecto, facturasVenta, advertencias);

        BigDecimal margenRealArs = ingresos.cobradoArs()
                .subtract(egresos.pagadoArs())
                .subtract(comisionesArs)
                .subtract(impuestosAtribuidosArs);

        return new ReporteRentabilidadProyectoResponse(
                proyecto.getId(), proyecto.getNombre(), proyecto.getCliente().getNombre(),
                proyecto.getTipoProyecto() != null ? proyecto.getTipoProyecto().name() : null,
                proyecto.getEstado().name(), proyecto.getFechaEstimadaFinalizacion(), proyecto.getFechaRealFinalizacion(),
                etapasResumen,
                ingresos.facturadoArs(), ingresos.cobradoArs(), ingresos.pendienteArs(),
                facturasVenta.size(), ingresos.facturasSaldadas(),
                egresos.facturadoArs(), egresos.pagadoArs(), egresos.pendienteArs(), egresos.proveedores(),
                comisionesResumen, List.copyOf(comisionesPorMoneda.values()), comisionesArs,
                impuestosAtribuidosArs,
                presupuestoComparacion,
                margenRealArs,
                advertencias);
    }

    private record Ingresos(BigDecimal facturadoArs, BigDecimal cobradoArs, BigDecimal pendienteArs, int facturasSaldadas) {}

    private Ingresos calcularIngresos(List<FacturaVenta> facturas) {
        if (facturas.isEmpty()) {
            return new Ingresos(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
        List<Long> ids = facturas.stream().map(FacturaVenta::getId).toList();
        Map<Long, BigDecimal> cobradoPorFactura = new HashMap<>();
        for (ImputadoFacturaVenta i : cobroImputacionRepo.sumarImputadoPorFactura(ids, EstadoDocumento.CONFIRMADO)) {
            cobradoPorFactura.merge(i.facturaVentaId(), i.imputadoArs(), BigDecimal::add);
        }
        for (ImputadoFacturaVenta i : aplicacionAnticipoClienteRepo.sumarAplicacionesPorFactura(ids)) {
            cobradoPorFactura.merge(i.facturaVentaId(), i.imputadoArs(), BigDecimal::add);
        }

        BigDecimal facturado = BigDecimal.ZERO;
        BigDecimal cobrado = BigDecimal.ZERO;
        int saldadas = 0;
        for (FacturaVenta f : facturas) {
            facturado = facturado.add(f.getTotalArs());
            BigDecimal cobradoFactura = cobradoPorFactura.getOrDefault(f.getId(), BigDecimal.ZERO);
            cobrado = cobrado.add(cobradoFactura);
            if (cobradoFactura.compareTo(f.getTotalArs()) >= 0) {
                saldadas++;
            }
        }
        return new Ingresos(facturado, cobrado, facturado.subtract(cobrado), saldadas);
    }

    private record Egresos(BigDecimal facturadoArs, BigDecimal pagadoArs, BigDecimal pendienteArs, List<ProveedorResumen> proveedores) {}

    private Egresos calcularEgresos(List<FacturaCompra> facturas) {
        if (facturas.isEmpty()) {
            return new Egresos(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        }
        List<Long> ids = facturas.stream().map(FacturaCompra::getId).toList();
        Map<Long, BigDecimal> pagadoPorFactura = new HashMap<>();
        for (ImputadoFacturaCompra i : pagoImputacionRepo.sumarImputadoPorFactura(ids, EstadoDocumento.CONFIRMADO)) {
            pagadoPorFactura.merge(i.facturaCompraId(), i.imputadoArs(), BigDecimal::add);
        }
        for (ImputadoFacturaCompra i : aplicacionAnticipoProveedorRepo.sumarAplicacionesPorFactura(ids)) {
            pagadoPorFactura.merge(i.facturaCompraId(), i.imputadoArs(), BigDecimal::add);
        }

        BigDecimal facturado = BigDecimal.ZERO;
        BigDecimal pagado = BigDecimal.ZERO;
        Map<Long, ProveedorAcumulado> porProveedor = new LinkedHashMap<>();
        for (FacturaCompra f : facturas) {
            facturado = facturado.add(f.getTotalArs());
            BigDecimal pagadoFactura = pagadoPorFactura.getOrDefault(f.getId(), BigDecimal.ZERO);
            pagado = pagado.add(pagadoFactura);

            Long proveedorId = f.getProveedor().getId();
            ProveedorAcumulado acc = porProveedor.computeIfAbsent(proveedorId,
                    id -> new ProveedorAcumulado(id, f.getProveedor().getNombre()));
            acc.facturadoArs = acc.facturadoArs.add(f.getTotalArs());
            acc.pagadoArs = acc.pagadoArs.add(pagadoFactura);
        }
        List<ProveedorResumen> proveedores = porProveedor.values().stream()
                .map(a -> new ProveedorResumen(a.proveedorId, a.proveedorNombre, a.facturadoArs, a.pagadoArs,
                        a.facturadoArs.subtract(a.pagadoArs)))
                .toList();
        return new Egresos(facturado, pagado, facturado.subtract(pagado), proveedores);
    }

    private static final class ProveedorAcumulado {
        final Long proveedorId;
        final String proveedorNombre;
        BigDecimal facturadoArs = BigDecimal.ZERO;
        BigDecimal pagadoArs = BigDecimal.ZERO;

        ProveedorAcumulado(Long proveedorId, String proveedorNombre) {
            this.proveedorId = proveedorId;
            this.proveedorNombre = proveedorNombre;
        }
    }

    /**
     * Empareja por orden cada cuota pactada con la N-ésima factura de venta
     * confirmada (por fecha ascendente) y convierte esa porción del
     * presupuesto (USD) con el TC de esa factura puntual. Las cuotas sin
     * factura real todavía quedan fuera de la comparación.
     */
    private PresupuestoComparacion calcularComparacionPresupuesto(Proyecto proyecto, List<FacturaVenta> facturasVenta, List<String> advertencias) {
        var presupuestoOpt = presupuestoProyectoService.obtener(proyecto.getId());
        if (presupuestoOpt.isEmpty()) {
            advertencias.add("El proyecto no tiene presupuesto cargado (F2.6): no hay comparación presupuestado vs. real.");
            return null;
        }
        PresupuestoProyecto presupuesto = presupuestoOpt.get();
        PresupuestoCalculado calculado = presupuestoProyectoService.calcular(presupuesto);

        Integer cantidadPagosPactados = proyecto.getCantidadPagosPactados();
        int cantidadPagos = cantidadPagosPactados != null && cantidadPagosPactados > 0
                ? cantidadPagosPactados
                : Math.max(proyecto.getCuotas().size(), 1);

        List<ProyectoCuota> cuotasOrdenadas = proyecto.getCuotas().stream()
                .sorted(Comparator.comparing(ProyectoCuota::getNumero))
                .toList();
        List<FacturaVenta> facturasOrdenadas = facturasVenta.stream()
                .sorted(Comparator.comparing(FacturaVenta::getFecha))
                .toList();

        int emparejados = Math.min(Math.min(cuotasOrdenadas.size(), facturasOrdenadas.size()), cantidadPagos);
        if (emparejados < cantidadPagos) {
            advertencias.add("Presupuesto: %d de %d pagos todavía no tienen factura real emparejada — esa porción no se convirtió a pesos."
                    .formatted(cantidadPagos - emparejados, cantidadPagos));
        }

        BigDecimal montoPorPagoUsd = calculado.precioFinalCliente()
                .divide(BigDecimal.valueOf(cantidadPagos), ESCALA_INTERNA, RoundingMode.HALF_UP);

        BigDecimal presupuestoConvertidoArs = BigDecimal.ZERO;
        BigDecimal facturadoEmparejadoArs = BigDecimal.ZERO;
        for (int i = 0; i < emparejados; i++) {
            FacturaVenta factura = facturasOrdenadas.get(i);
            presupuestoConvertidoArs = presupuestoConvertidoArs.add(
                    montoPorPagoUsd.multiply(factura.getTipoCambio()).setScale(2, RoundingMode.HALF_UP));
            facturadoEmparejadoArs = facturadoEmparejadoArs.add(factura.getTotalArs());
        }

        return new PresupuestoComparacion(calculado, cantidadPagos, emparejados,
                presupuestoConvertidoArs, facturadoEmparejadoArs, facturadoEmparejadoArs.subtract(presupuestoConvertidoArs));
    }
}
