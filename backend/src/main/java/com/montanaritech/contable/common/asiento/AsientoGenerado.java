package com.montanaritech.contable.common.asiento;

import java.util.List;

/**
 * Resultado de un {@link AsientoGenerator}: ya validado (Σdebe = Σhaber),
 * numerado y vinculado a su documento origen. El motor contable real
 * (F3.1) es quien lo persiste como asiento confirmado.
 */
public record AsientoGenerado(
        Long numeroInterno,
        List<LineaAsientoGenerada> lineas,
        String documentoOrigenTipo,
        Long documentoOrigenId
) {
}
