package com.montanaritech.contable.contabilidad.asiento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AsientoResponse(
        Long id,
        LocalDate fecha,
        String descripcion,
        String estado,
        Long numero,
        String origen,
        String origenTipo,
        Long origenId,
        String observaciones,
        String motivoAnulacion,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        List<LineaResponse> lineas
) {
    public record LineaResponse(
            Long id,
            Integer orden,
            Long cuentaContableId,
            String cuentaContableCodigo,
            String cuentaContableNombre,
            BigDecimal debe,
            BigDecimal haber,
            Long monedaId,
            String monedaCodigo,
            BigDecimal tipoCambio,
            BigDecimal importeOriginal,
            String fuenteTc,
            String leyenda,
            Long proyectoId,
            String proyectoNombre,
            Long etapaId,
            String etapaNombre,
            Long clienteId,
            String clienteNombre,
            Long proveedorId,
            String proveedorNombre,
            Long cuentaBancariaId,
            String cuentaBancariaAlias,
            boolean generadaAuto
    ) {}
}
