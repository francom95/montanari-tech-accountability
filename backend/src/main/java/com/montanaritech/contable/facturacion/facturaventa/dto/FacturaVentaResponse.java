package com.montanaritech.contable.facturacion.facturaventa.dto;

import com.montanaritech.contable.facturacion.TipoComprobante;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FacturaVentaResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        Long proyectoId,
        String proyectoNombre,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        TipoComprobante tipoComprobante,
        String puntoVenta,
        String numero,
        Long jurisdiccionDestinoId,
        Long monedaId,
        String monedaCodigo,
        BigDecimal tipoCambio,
        BigDecimal netoGravado,
        BigDecimal noGravado,
        BigDecimal exento,
        BigDecimal importeIva,
        BigDecimal total,
        BigDecimal totalArs,
        String estado,
        Long asientoId,
        Long asientoNumero,
        String observaciones,
        List<LineaResponse> lineas
) {
    public record LineaResponse(
            Long id,
            Integer orden,
            String descripcion,
            String tipo,
            BigDecimal importeNeto,
            BigDecimal alicuotaIva,
            BigDecimal importeIva,
            String tipoIngreso,
            Long cuentaContableId,
            String cuentaContableCodigo
    ) {}
}
