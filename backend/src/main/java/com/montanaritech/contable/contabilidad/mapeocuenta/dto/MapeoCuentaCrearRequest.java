package com.montanaritech.contable.contabilidad.mapeocuenta.dto;

import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import jakarta.validation.constraints.NotNull;

public record MapeoCuentaCrearRequest(
        @NotNull ConceptoContable concepto,
        String discriminadorTipo,
        String discriminadorValor,
        @NotNull Long cuentaContableId
) {}
