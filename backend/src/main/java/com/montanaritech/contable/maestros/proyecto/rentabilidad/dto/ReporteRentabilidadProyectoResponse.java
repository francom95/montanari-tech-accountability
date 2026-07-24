package com.montanaritech.contable.maestros.proyecto.rentabilidad.dto;

import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoCalculado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de rentabilidad por proyecto (F7.4): agrega — sin recalcular —
 * presupuesto (F2.6), comisiones (F2.7), facturación/cobros/pagos (F4.4/F4.5)
 * e impuestos atribuidos (F6.3). Ingresos/egresos/comisiones-en-ARS/impuestos
 * siempre en ARS (misma moneda funcional que el resto de los reportes); el
 * presupuesto es en USD y solo se convierte para la porción ya facturada
 * (ver {@link PresupuestoComparacion}).
 */
public record ReporteRentabilidadProyectoResponse(
        Long proyectoId,
        String proyectoNombre,
        String clienteNombre,
        String tipoProyecto,
        String estado,
        LocalDate fechaEstimadaFinalizacion,
        LocalDate fechaRealFinalizacion,
        List<EtapaResumen> etapas,

        BigDecimal totalFacturadoVentaArs,
        BigDecimal totalCobradoArs,
        BigDecimal pendienteCobroArs,
        int facturasVentaConfirmadas,
        int facturasVentaSaldadas,

        BigDecimal totalFacturadoCompraArs,
        BigDecimal totalPagadoArs,
        BigDecimal pendientePagoArs,
        List<ProveedorResumen> proveedores,

        List<ComisionResumen> comisiones,
        List<TotalPorMoneda> comisionesPorMoneda,
        BigDecimal comisionesArs,

        BigDecimal impuestosAtribuidosArs,

        PresupuestoComparacion presupuesto,

        BigDecimal margenRealArs,
        List<String> advertencias
) {
    public record EtapaResumen(Long id, String nombre, String estado, Integer porcentajeAvance) {}

    public record ProveedorResumen(Long proveedorId, String proveedorNombre,
            BigDecimal facturadoArs, BigDecimal pagadoArs, BigDecimal pendienteArs) {}

    public record ComisionResumen(Long id, String comisionistaNombre, BigDecimal porcentajeComision,
            String estadoPago, BigDecimal importeEstimado, BigDecimal importeFinal, String monedaCodigo) {}

    public record TotalPorMoneda(Long monedaId, String monedaCodigo, BigDecimal total) {}

    /**
     * Emparejamiento por orden entre las cuotas pactadas (F2.5) y las
     * facturas de venta confirmadas del proyecto: la cuota N usa el TC de la
     * N-ésima factura por fecha. Las cuotas sin factura real todavía quedan
     * fuera de {@code presupuestoConvertidoArs}/{@code facturadoEmparejadoArs}
     * — no se les asume un TC.
     */
    public record PresupuestoComparacion(
            PresupuestoCalculado calculado,
            int cantidadPagosPactados,
            int pagosEmparejadosConFactura,
            BigDecimal presupuestoConvertidoArs,
            BigDecimal facturadoEmparejadoArs,
            BigDecimal diferenciaArs
    ) {}
}
