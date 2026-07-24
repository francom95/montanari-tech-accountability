package com.montanaritech.contable.compromiso;

/** Estado del compromiso presupuestado (F8.2) — independiente de {@code activo} (soft-delete de PL-1). */
public enum EstadoCompromiso {
    PENDIENTE,
    RESUELTO,
    CANCELADO
}
