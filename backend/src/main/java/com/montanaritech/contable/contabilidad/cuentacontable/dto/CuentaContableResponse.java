package com.montanaritech.contable.contabilidad.cuentacontable.dto;

import java.util.Set;

public record CuentaContableResponse(
        Long id,
        String codigo,
        String nombre,
        Long padreId,
        String padreCodigo,
        String naturaleza,
        Long rubroId,
        String rubroNombre,
        boolean imputable,
        String saldoEsperado,
        boolean activo,
        Set<ProyectoUsoHabitualDto> proyectosUsoHabitual
) {
    public record ProyectoUsoHabitualDto(Long id, String nombre) {}
}
