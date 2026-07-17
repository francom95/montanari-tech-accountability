package com.montanaritech.contable.contabilidad.asiento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * {@code tipoCambio}/{@code importeOriginal} son opcionales en el DTO: para
 * líneas ARS el service los normaliza a 1 y a debe+haber al confirmar (F3.1
 * §3.3); para líneas en moneda extranjera, si se omite el TC, el service
 * intenta resolverlo automáticamente (F3.1 §3.4 ítem 3, CP-19).
 */
public record AsientoLineaRequest(
        @NotNull Long cuentaContableId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal debe,
        @NotNull @DecimalMin(value = "0.00") BigDecimal haber,
        @NotNull Long monedaId,
        BigDecimal tipoCambio,
        BigDecimal importeOriginal,
        String leyenda,
        Long proyectoId,
        Long etapaId,
        Long clienteId,
        Long proveedorId,
        Long cuentaBancariaId
) {}
