package com.montanaritech.contable.impuestos.atribucion;

/**
 * Cómo se reparte un impuesto liquidado entre proyectos (F6.3).
 *
 * <ul>
 *   <li>{@link #DIRECTO}: 100% a un solo proyecto.</li>
 *   <li>{@link #FACTURACION}: proporcional a la facturación (ventas netas) de
 *       cada proyecto en el período de la liquidación.</li>
 *   <li>{@link #MARGEN}: proporcional al margen (ventas − compras) de cada
 *       proyecto en el período.</li>
 *   <li>{@link #PORCENTAJE_MANUAL}: porcentajes cargados a mano (deben sumar 100).</li>
 * </ul>
 */
public enum CriterioAtribucion {
    DIRECTO,
    FACTURACION,
    MARGEN,
    PORCENTAJE_MANUAL
}
