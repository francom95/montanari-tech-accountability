package com.montanaritech.contable.pendiente.importacion;

/**
 * Fila cruda de las hojas "PENDIENTES"/"PENDIENTES AHORA" (F10.1 §14): una
 * sola columna, el texto de la nota → {@code PendienteAdministrativo.titulo}.
 * Carga directa, sin transformación: {@code categoria=null}, {@code fechaEstimadaResolucion=null},
 * {@code prioridad=MEDIA}.
 */
public record PendienteAdministrativoImportFilaCruda(
        int numeroFila,
        String titulo
) {}
