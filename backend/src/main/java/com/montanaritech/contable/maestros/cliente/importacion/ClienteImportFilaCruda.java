package com.montanaritech.contable.maestros.cliente.importacion;

/**
 * Fila cruda de la hoja "Base de datos - Clientes" (F10.1 §2), columnas fijas
 * en orden: {@code Nombre Clientes}, {@code Razón Social}, {@code CUIT}.
 */
public record ClienteImportFilaCruda(
        int numeroFila,
        String nombreClientes,
        String razonSocial,
        String cuit
) {}
