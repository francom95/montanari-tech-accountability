package com.montanaritech.contable.contabilidad.cuentacontable.dto;

import java.util.List;

/** Nodo del árbol del plan de cuentas (F3.2): la jerarquía completa se arma en memoria (ver {@code ArbolCuentasService}). */
public record CuentaContableNodo(
        Long id,
        String codigo,
        String nombre,
        String naturaleza,
        Long rubroId,
        String rubroNombre,
        boolean imputable,
        String saldoEsperado,
        boolean activo,
        List<CuentaContableNodo> hijos
) {}
