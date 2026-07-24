package com.montanaritech.contable.contabilidad.mapeocuenta;

/**
 * Catálogo cerrado de conceptos contables que un {@code AsientoGenerator}
 * resuelve vía {@link ResolutorCuentas} (F4.1 §1.3). Se predeclara completo
 * aunque F4.2 (facturas de venta) solo use {@code IVA_DEBITO_FISCAL},
 * {@code INGRESO_VENTA} y {@code CREDITO_POR_VENTA} — mismo criterio que
 * {@code OrigenAsiento}/{@code EstadoDocumento} del molde F1.8: el dominio ya
 * está cerrado por el diseño de F4.1, y agregar valores no pide migración
 * (columna VARCHAR).
 *
 * <p>Los conceptos "practicada" (retención/percepción que Montanari le
 * aplicaría a un tercero) se descartaron en el checkpoint de F4.1: Montanari
 * no es agente de retención/percepción. Solo existen las "sufridas".
 */
public enum ConceptoContable {
    IVA_DEBITO_FISCAL,
    IVA_CREDITO_FISCAL,
    INGRESO_VENTA,
    CREDITO_POR_VENTA,
    DEUDA_COMERCIAL,
    COSTO_GASTO,
    PERCEPCION_IVA_SUFRIDA,
    PERCEPCION_IIBB_SUFRIDA,
    RETENCION_GANANCIAS_SUFRIDA,
    RETENCION_IVA_SUFRIDA,
    DIF_CAMBIO_GANADA,
    DIF_CAMBIO_PERDIDA,
    ANTICIPO_CLIENTE,
    ANTICIPO_PROVEEDOR,
    /**
     * Imputación rápida de movimientos bancarios sin match (F5.3 §1: comisiones,
     * impuesto Ley 25413). SIRCREB y percepciones bancarias NO suman conceptos
     * nuevos acá — son la misma percepción de IIBB/IVA "sufrida" que ya
     * resuelven {@code PERCEPCION_IIBB_SUFRIDA}/{@code PERCEPCION_IVA_SUFRIDA}
     * en facturas de compra (F4.3), solo que originada en un movimiento
     * bancario en vez de una factura.
     */
    COMISION_BANCARIA,
    IMPUESTO_DEBITOS_CREDITOS_BANCARIOS,
    /**
     * Resultado de la liquidación mensual de IVA (F6.1). {@code IVA_SALDO_A_PAGAR}
     * mapea a 2.1.2009, que ya existía en el seed de F3.3 sin uso esperando este
     * paso; {@code IVA_SALDO_A_FAVOR} necesitó cuenta nueva (1.1.2014) y es el
     * acumulador que se arrastra al período siguiente.
     */
    IVA_SALDO_A_PAGAR,
    /**
     * Saldo <b>técnico</b> a favor (art. 24, 1er párrafo de la Ley 23.349): solo
     * computable contra débitos fiscales de períodos siguientes. Cuenta 1.1.2014.
     */
    IVA_SALDO_A_FAVOR,
    /**
     * Saldo de <b>libre disponibilidad</b> (art. 24, 2do párrafo): el excedente
     * de ingresos directos (percepciones y retenciones sufridas) por sobre el
     * impuesto determinado. A diferencia del técnico, además se compensa con
     * otros impuestos, se transfiere y se puede pedir devuelto. Cuenta 1.1.2015.
     */
    IVA_SALDO_LIBRE_DISPONIBILIDAD,
    /**
     * Liquidación mensual de IIBB (F6.2). Las tres cuentas ya existían en el
     * seed de F3.3 sin mapeo, esperando este paso: {@code IMPUESTO_IIBB_DETERMINADO}
     * → 5.3.2009 (el gasto del período), {@code IIBB_A_PAGAR} → 2.1.2010 (el
     * pasivo) e {@code IIBB_SALDO_A_FAVOR} → 1.1.2008 (el arrastre y las
     * deducciones sufridas, que reusan {@code PERCEPCION_IIBB_SUFRIDA}).
     */
    IMPUESTO_IIBB_DETERMINADO,
    IIBB_A_PAGAR,
    IIBB_SALDO_A_FAVOR,
    /**
     * Recargo por mora en un cobro que llega después del vencimiento de la
     * factura (F7.4). Mapea a 6.4002 "Intereses Ganados", sembrada en F3.3
     * sin uso hasta ahora.
     */
    INTERES_POR_MORA_GANADO
}
