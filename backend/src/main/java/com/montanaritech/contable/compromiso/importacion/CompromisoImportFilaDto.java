package com.montanaritech.contable.compromiso.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompromisoImportFilaDto(
        int fila,
        String concepto,
        LocalDate fechaPrevista,
        BigDecimal importe,
        List<String> errores
) {}
