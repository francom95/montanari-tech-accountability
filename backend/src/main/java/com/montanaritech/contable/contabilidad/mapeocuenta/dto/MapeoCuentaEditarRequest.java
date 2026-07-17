package com.montanaritech.contable.contabilidad.mapeocuenta.dto;

import jakarta.validation.constraints.NotNull;

public record MapeoCuentaEditarRequest(@NotNull Long cuentaContableId) {}
