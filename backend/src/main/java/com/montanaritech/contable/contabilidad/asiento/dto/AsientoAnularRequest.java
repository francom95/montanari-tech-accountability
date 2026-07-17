package com.montanaritech.contable.contabilidad.asiento.dto;

import jakarta.validation.constraints.NotBlank;

/** Anulación por marca (F3.1 §4.4): el motivo es obligatorio. */
public record AsientoAnularRequest(@NotBlank String motivo) {}
