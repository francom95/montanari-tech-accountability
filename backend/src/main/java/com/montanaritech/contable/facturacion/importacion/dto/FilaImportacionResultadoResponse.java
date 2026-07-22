package com.montanaritech.contable.facturacion.importacion.dto;

/**
 * Resultado de confirmar una fila (F4.6). {@code exito=true} incluye
 * {@code facturaId}/{@code estadoFinal} (el borrador se crea siempre que
 * pasa las validaciones; si se pidió CONFIRMADO y la confirmación
 * automática falla — p.ej. falta un mapeo de cuenta — el borrador queda
 * creado igual y {@code advertencia} explica por qué no se confirmó).
 * {@code exito=false} significa que ni el borrador se pudo crear
 * (rechazo real: duplicado, datos inválidos, etc.).
 */
public record FilaImportacionResultadoResponse(
        String nombreArchivo,
        boolean exito,
        String tipo,
        String numero,
        Long facturaId,
        String estadoFinal,
        Long asientoId,
        String motivoRechazo,
        String advertencia
) {}
