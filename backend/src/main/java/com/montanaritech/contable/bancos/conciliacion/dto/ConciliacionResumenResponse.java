package com.montanaritech.contable.bancos.conciliacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConciliacionResumenResponse(
        Long cuentaBancariaId,
        String cuentaBancariaAlias,
        String monedaCodigo,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        /** Saldo inicial + movimientos bancarios (no descartados) hasta fechaHasta — "lo que dice el extracto". */
        BigDecimal saldoBanco,
        /** Saldo de la cuenta contable espejo, asientos CONFIRMADO hasta fechaHasta (Mayor, F3.6) — "lo que dice el sistema". */
        BigDecimal saldoSistema,
        BigDecimal diferencia,
        List<ConciliacionMovimientoResponse> movimientos
) {}
