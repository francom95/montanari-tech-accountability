package com.montanaritech.contable.facturacion.facturacompra.dto;

import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FacturaCompraResponse(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        Long proyectoId,
        String proyectoNombre,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        TipoComprobante tipoComprobante,
        String puntoVenta,
        String numero,
        Long monedaId,
        String monedaCodigo,
        BigDecimal tipoCambio,
        BigDecimal neto,
        BigDecimal importeIva,
        BigDecimal importePercepciones,
        BigDecimal total,
        BigDecimal totalArs,
        String estado,
        Long asientoId,
        Long asientoNumero,
        String observaciones,
        List<LineaResponse> lineas,
        List<TributoResponse> tributos
) {
    public record LineaResponse(
            Long id,
            Integer orden,
            String descripcion,
            Long tipoCostoId,
            String tipoCostoNombre,
            BigDecimal importeNeto,
            BigDecimal alicuotaIva,
            BigDecimal importeIva,
            Long cuentaContableId,
            String cuentaContableCodigo
    ) {}

    public record TributoResponse(
            Long id,
            TipoTributo tipo,
            Long jurisdiccionId,
            String jurisdiccionNombre,
            BigDecimal base,
            BigDecimal alicuota,
            BigDecimal importe
    ) {}
}
