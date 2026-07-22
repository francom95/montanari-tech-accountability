package com.montanaritech.contable.facturacion.cobro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Crea siempre un BORRADOR (F3.1 §3.5). {@code total} es el bruto cobrado en
 * moneda original; {@code lineas} son las imputaciones contra facturas
 * (Σ lineas ≤ total — el remanente es anticipo, F4.1 §6.5). Sin líneas ⇒
 * cobro 100% anticipo, no es un caso especial.
 */
public record CobroCrearRequest(
        @NotNull Long clienteId,
        @NotNull LocalDate fecha,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        @NotNull Long cuentaBancariaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal total,
        String observaciones,
        @Valid List<CobroImputacionRequest> lineas,
        @Valid List<CobroTributoRequest> tributos
) {}
