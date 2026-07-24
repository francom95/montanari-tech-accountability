package com.montanaritech.contable.compromiso.dto;

import com.montanaritech.contable.compromiso.EstadoCompromiso;
import com.montanaritech.contable.compromiso.TipoCompromiso;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CompromisoResponse(
        Long id,
        String concepto,
        TipoCompromiso tipo,
        LocalDate fechaPrevista,
        BigDecimal importe,
        Long monedaId,
        String monedaCodigo,
        Long proveedorId,
        String proveedorNombre,
        Long proyectoId,
        String proyectoNombre,
        EstadoCompromiso estado,
        String observaciones,
        Long vencimientoGeneradoId,
        boolean activo
) {}
