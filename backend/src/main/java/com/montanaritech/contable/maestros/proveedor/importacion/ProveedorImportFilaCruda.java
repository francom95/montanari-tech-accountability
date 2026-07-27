package com.montanaritech.contable.maestros.proveedor.importacion;

/**
 * Fila cruda de la hoja "Proveedores de servicios" (F10.1 §3), columnas fijas
 * en orden: {@code Proveedor} (nombre), {@code CUIT} (columna hoy ausente en
 * la hoja real, pero el parser la deja preparada para una extracción futura
 * que sí la traiga).
 */
public record ProveedorImportFilaCruda(
        int numeroFila,
        String nombre,
        String cuit
) {}
