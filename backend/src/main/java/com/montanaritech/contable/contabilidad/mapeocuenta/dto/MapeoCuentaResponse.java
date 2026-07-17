package com.montanaritech.contable.contabilidad.mapeocuenta.dto;

import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;

public record MapeoCuentaResponse(
        Long id,
        ConceptoContable concepto,
        String discriminadorTipo,
        String discriminadorValor,
        Long cuentaContableId,
        String cuentaContableCodigo,
        String cuentaContableNombre,
        boolean activo
) {}
