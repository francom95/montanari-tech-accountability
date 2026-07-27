package com.montanaritech.contable.inversion.importacion;

import com.montanaritech.contable.inversion.TipoMovimientoInversion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InversionImportFilaDto(
        int fila,
        String instrumento,
        String objetivoDelDinero,
        TipoMovimientoInversion tipo,
        LocalDate fecha,
        BigDecimal cuotapartes,
        BigDecimal valorCuotaparte,
        BigDecimal montoAplicado,
        List<String> errores
) {}
