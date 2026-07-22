package com.montanaritech.contable.bancos.movimientobancario;

/**
 * De dónde llegó el movimiento bancario (F5.1). Predeclarado completo:
 * F5.1 solo emite {@code MANUAL} (carga a mano); F5.2 (parsers de
 * resúmenes Galicia/Mercado Pago/tarjeta) reusa el resto tal cual.
 */
public enum OrigenImportacionMovimiento {
    MANUAL,
    GALICIA,
    MERCADO_PAGO,
    TARJETA_CREDITO
}
