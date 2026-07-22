package com.montanaritech.contable.facturacion.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PagoResponse(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        LocalDate fecha,
        Long monedaId,
        String monedaCodigo,
        BigDecimal tipoCambio,
        Long cuentaBancariaId,
        String cuentaBancariaAlias,
        BigDecimal total,
        BigDecimal totalArs,
        BigDecimal montoAnticipo,
        BigDecimal montoAnticipoDisponible,
        String estado,
        Long asientoId,
        Long asientoNumero,
        String observaciones,
        List<ImputacionResponse> lineas,
        List<AplicacionAnticipoResponse> aplicacionesAnticipo
) {
    public record ImputacionResponse(
            Long id,
            Integer orden,
            Long facturaCompraId,
            String facturaCompraNumero,
            BigDecimal montoImputadoOriginal,
            BigDecimal montoArsCancelado
    ) {}

    public record AplicacionAnticipoResponse(
            Long id,
            Long facturaCompraId,
            String facturaCompraNumero,
            LocalDate fecha,
            BigDecimal montoOriginal,
            BigDecimal montoArsCancelado,
            Long asientoId,
            Long asientoNumero
    ) {}
}
