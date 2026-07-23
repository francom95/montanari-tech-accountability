package com.montanaritech.contable.impuestos.atribucion;

/**
 * A qué liquidación apunta una atribución (F6.3). Referencia polimórfica sin FK
 * real ({@code liquidacion_tipo} + {@code liquidacion_id}), mismo patrón que
 * {@code ComprobanteTributo} — así una atribución sirve para IVA y para IIBB sin
 * tocar esas entidades.
 */
public enum TipoLiquidacion {
    IVA,
    IIBB
}
