package com.montanaritech.contable.inversion.importacion;

/**
 * Fila cruda de la hoja "Inversiones en Fondos Fima" (F10.1 §6), columnas
 * fijas en orden: {@code Fondo}, {@code Detalle}, {@code Operación}
 * ("Agregar"/"Retirar"/vacío — vacío = revaluación, no es un movimiento real),
 * {@code Fecha de liquidación}, {@code Cuotapartes}, {@code Valor cuotaparte}, {@code Monto}.
 */
public record InversionImportFilaCruda(
        int numeroFila,
        String fondo,
        String detalle,
        String operacion,
        String fechaLiquidacion,
        String cuotapartes,
        String valorCuotaparte,
        String monto
) {}
