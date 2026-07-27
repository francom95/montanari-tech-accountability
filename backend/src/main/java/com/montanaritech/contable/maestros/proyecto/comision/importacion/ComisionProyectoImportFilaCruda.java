package com.montanaritech.contable.maestros.proyecto.comision.importacion;

/**
 * Fila cruda de la hoja "Comisiones por ventas" (F10.1 §4), columnas fijas en
 * orden: {@code Proyecto}, {@code Comisionista}, {@code % Comisión} (ausente
 * en 12 de 14 filas), {@code Monto total de la Comisión}, {@code Comentarios}
 * (a veces indica "En dols" — moneda USD).
 */
public record ComisionProyectoImportFilaCruda(
        int numeroFila,
        String proyecto,
        String comisionista,
        String porcentaje,
        String monto,
        String comentarios
) {}
