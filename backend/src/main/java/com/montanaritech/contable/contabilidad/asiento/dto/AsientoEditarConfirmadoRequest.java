package com.montanaritech.contable.contabilidad.asiento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Edita un asiento ya {@code CONFIRMADO} (F3.1 §4.2): a diferencia del
 * borrador, esto re-corre el checklist de confirmación completo (balanceo,
 * XOR, cuenta imputable/activa, consistencia ARS) porque el asiento sigue
 * impactando reportes — solo que sin reasignar número ni cambiar estado.
 */
public record AsientoEditarConfirmadoRequest(
        @NotNull LocalDate fecha,
        @NotBlank String descripcion,
        String observaciones,
        @Valid List<AsientoLineaEditarRequest> lineas
) {}
