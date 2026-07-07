package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

/**
 * Fila cruda tal cual sale del archivo (todo String, sin validar). Columnas
 * fijas en este orden: nombre, descripción, estado, fecha inicio (dd/MM/yyyy),
 * fecha estimada fin (dd/MM/yyyy), % avance, monto presupuestado, costos
 * estimados, pagos previstos, cobros previstos, observaciones, proveedores
 * (nombres separados por ';').
 */
public record EtapaImportFilaCruda(
        int numeroFila,
        String nombre,
        String descripcion,
        String estado,
        String fechaInicio,
        String fechaEstimadaFin,
        String porcentajeAvance,
        String montoPresupuestado,
        String costosEstimados,
        String pagosPrevistos,
        String cobrosPrevistos,
        String observaciones,
        String proveedores
) {}
