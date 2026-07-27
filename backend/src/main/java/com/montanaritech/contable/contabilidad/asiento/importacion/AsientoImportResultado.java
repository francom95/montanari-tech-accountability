package com.montanaritech.contable.contabilidad.asiento.importacion;

import java.util.List;

/**
 * {@code yaExistian}: cantidad de asientos (no líneas) cuya clave natural
 * (fecha + descripción con el N° de Excel embebido) ya existía — idempotencia.
 * {@code rechazadas}: líneas individuales, incluidas las que no tenían error
 * propio pero pertenecían a un asiento rechazado por otra línea del mismo
 * grupo (un asiento no puede migrar parcialmente).
 */
public record AsientoImportResultado(
        List<AsientoImportCreadoDto> creados,
        List<AsientoImportFilaDto> rechazadas,
        int yaExistian
) {
    public record AsientoImportCreadoDto(String numeroAsientoOriginal, Long asientoId, Long numero, java.time.LocalDate fecha, int cantidadLineas) {}
}
