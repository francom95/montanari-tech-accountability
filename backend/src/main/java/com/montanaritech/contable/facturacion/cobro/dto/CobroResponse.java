package com.montanaritech.contable.facturacion.cobro.dto;

import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CobroResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        LocalDate fecha,
        Long monedaId,
        String monedaCodigo,
        BigDecimal tipoCambio,
        Long cuentaBancariaId,
        String cuentaBancariaAlias,
        BigDecimal total,
        BigDecimal totalArs,
        BigDecimal importeRetenciones,
        BigDecimal montoAnticipo,
        BigDecimal montoAnticipoDisponible,
        String estado,
        Long asientoId,
        Long asientoNumero,
        String observaciones,
        List<ImputacionResponse> lineas,
        List<TributoResponse> tributos,
        List<AplicacionAnticipoResponse> aplicacionesAnticipo
) {
    public record ImputacionResponse(
            Long id,
            Integer orden,
            Long facturaVentaId,
            String facturaVentaNumero,
            BigDecimal montoImputadoOriginal,
            BigDecimal montoArsCancelado
    ) {}

    public record TributoResponse(
            Long id,
            TipoTributo tipo,
            BigDecimal importe
    ) {}

    public record AplicacionAnticipoResponse(
            Long id,
            Long facturaVentaId,
            String facturaVentaNumero,
            LocalDate fecha,
            BigDecimal montoOriginal,
            BigDecimal montoArsCancelado,
            Long asientoId,
            Long asientoNumero
    ) {}
}
