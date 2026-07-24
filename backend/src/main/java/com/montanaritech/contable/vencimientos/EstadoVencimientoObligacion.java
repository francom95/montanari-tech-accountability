package com.montanaritech.contable.vencimientos;

/**
 * Estado persistido de un {@link Vencimiento} (F8.1). VENCIDO no es un valor
 * de esta columna: se computa en lectura (PENDIENTE + fecha pasada), mismo
 * criterio que {@code EstadoVencimiento} de F4.5 — ver
 * {@code VencimientoResponse.estado()}.
 */
public enum EstadoVencimientoObligacion {
    PENDIENTE,
    PAGADO,
    REPROGRAMADO,
    CANCELADO
}
