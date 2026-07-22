package com.montanaritech.contable.bancos.movimientobancario.dto;

import jakarta.validation.constraints.NotNull;

public record ImputarMovimientoBancarioRequest(@NotNull Long cuentaContableId) {}
