package com.montanaritech.contable.bancos.conciliacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConciliacionMovimientoResponse(
        Long movimientoBancarioId,
        LocalDate fecha,
        String descripcion,
        BigDecimal importe,
        String monedaCodigo,
        String estado,
        /** Presente solo si el movimiento está PENDIENTE y hay un asiento candidato sin asociar todavía. */
        MatchSugeridoResponse matchSugerido,
        /** Presente solo si está PENDIENTE, no hay match, y el clasificador reconoció la descripción. */
        CuentaSugeridaResponse cuentaSugerida,
        /** Presente solo si ya está CONCILIADO: el asiento con el que quedó vinculado. */
        Long asientoIdAsociado,
        Long asientoNumeroAsociado
) {}
