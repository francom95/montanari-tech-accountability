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
    ANTICIPO_PROVEEDOR
}
