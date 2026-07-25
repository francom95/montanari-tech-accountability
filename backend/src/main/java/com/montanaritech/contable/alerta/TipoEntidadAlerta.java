package com.montanaritech.contable.alerta;

/**
 * Referencia polimórfica sin FK (mismo patrón que {@code AtribucionImpuesto}
 * F6.3, {@code Vencimiento.origenGeneracion} F8.1, {@code Inversion.
 * vinculoTipo} F8.4): identifica a qué tabla apunta {@code Alerta.
 * entidadRefId}.
 */
public enum TipoEntidadAlerta {
    VENCIMIENTO,
    COMPROMISO,
    FACTURA_VENTA,
    FACTURA_COMPRA,
    CUENTA_BANCARIA,
    MOVIMIENTO_BANCARIO,
    PENDIENTE_ADMINISTRATIVO
}
