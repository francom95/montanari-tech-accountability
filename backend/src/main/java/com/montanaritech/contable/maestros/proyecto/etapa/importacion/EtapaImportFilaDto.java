package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fila ya parseada/validada, tal como la ve y puede corregir el usuario en la
 * previsualización antes de confirmar la importación. {@code errores} vacío
 * significa que la fila es válida para crear una {@code Etapa}.
 */
public record EtapaImportFilaDto(
        int fila,
        String nombre,
        String descripcion,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaEstimadaFin,
        Integer porcentajeAvance,
        BigDecimal montoPresupuestado,
        BigDecimal costosEstimados,
        BigDecimal pagosPrevistos,
        BigDecimal cobrosPrevistos,
        String observaciones,
        List<String> proveedoresNombres,
        List<Long> proveedoresIds,
        List<String> errores
) {}
