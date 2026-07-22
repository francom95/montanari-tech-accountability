package com.montanaritech.contable.bancos.movimientobancario.dto;

import jakarta.validation.constraints.NotNull;

/** El usuario identifica el asiento existente por su número visible (no el id interno). */
public record AsociarMovimientoBancarioRequest(@NotNull Long asientoNumero) {}
