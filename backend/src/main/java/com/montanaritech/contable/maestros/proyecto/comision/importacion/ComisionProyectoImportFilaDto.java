package com.montanaritech.contable.maestros.proyecto.comision.importacion;

import java.math.BigDecimal;
import java.util.List;

public record ComisionProyectoImportFilaDto(
        int fila,
        String proyectoNombre,
        String comisionistaNombre,
        BigDecimal porcentaje,
        BigDecimal monto,
        boolean esUsd,
        List<String> errores
) {}
