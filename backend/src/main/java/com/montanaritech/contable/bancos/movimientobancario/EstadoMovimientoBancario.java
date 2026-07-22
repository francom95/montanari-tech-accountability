package com.montanaritech.contable.bancos.movimientobancario;

/**
 * Estado de revisión de un movimiento bancario (F5.1). A diferencia de
 * {@code EstadoDocumento} (borrador/confirmado/anulado), acá el estado
 * inicial es el único "vivo": desde {@code PENDIENTE} el usuario decide
 * exactamente una vez, vía una de las 4 acciones (confirmar/asociar/imputar
 * generan {@code CONCILIADO}, descartar genera {@code DESCARTADO}) — ambos
 * terminales, sin reapertura (F5.1: "nada impacta la contabilidad hasta que
 * el usuario lo confirme explícitamente").
 */
public enum EstadoMovimientoBancario {
    PENDIENTE,
    CONCILIADO,
    DESCARTADO
}
