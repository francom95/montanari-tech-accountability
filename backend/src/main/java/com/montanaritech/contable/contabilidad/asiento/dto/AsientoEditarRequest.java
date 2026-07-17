package com.montanaritech.contable.contabilidad.asiento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Solo aplica a asientos en BORRADOR (editar un confirmado es F3.5). */
public record AsientoEditarRequest(
        @NotNull LocalDate fecha,
        @NotBlank String descripcion,
        String observaciones,
        @Valid List<AsientoLineaRequest> lineas
) {}
