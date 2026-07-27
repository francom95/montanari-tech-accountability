package com.montanaritech.contable.maestros.proyecto.importacion;

import java.math.BigDecimal;
import java.util.List;

public record ProyectoImportFilaDto(
        int fila,
        String clienteNombre,
        String proyectoNombre,
        String pais,
        String tipoProyecto,
        String estado,
        BigDecimal montoTotal,
        List<BigDecimal> cuotas,
        String comentarios,
        List<String> errores
) {}
