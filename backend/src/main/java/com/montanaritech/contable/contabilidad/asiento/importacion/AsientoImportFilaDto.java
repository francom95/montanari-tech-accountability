package com.montanaritech.contable.contabilidad.asiento.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AsientoImportFilaDto(
        int fila,
        String numeroAsientoOriginal,
        LocalDate fecha,
        Long cuentaContableId,
        String cuentaContableCodigo,
        BigDecimal debe,
        BigDecimal haber,
        String leyenda,
        List<String> errores
) {}
