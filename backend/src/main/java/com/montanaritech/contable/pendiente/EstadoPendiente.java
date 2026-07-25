package com.montanaritech.contable.pendiente;

/** Estado del pendiente administrativo (F8.5) — independiente de {@code activo} (soft-delete de PL-1). */
public enum EstadoPendiente {
    PENDIENTE,
    EN_PROCESO,
    RESUELTO,
    CANCELADO,
    POSTERGADO
}
