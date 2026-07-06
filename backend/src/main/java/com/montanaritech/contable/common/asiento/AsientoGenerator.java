package com.montanaritech.contable.common.asiento;

/**
 * Molde de referencia de PL-4 (F1.8): un generador por tipo de evento
 * confirmado que impacta contabilidad (factura de venta, factura de
 * compra, cobro, pago, liquidación de IVA/IIBB, resumen de tarjeta —
 * F3.x/F4.x/F6.x). Implementaciones futuras:
 * <ol>
 *   <li>Reciben el evento ya confirmado (nunca un borrador).</li>
 *   <li>Arman las líneas debe/haber según la tabla de mapeo
 *       concepto→cuenta (editable por admin, F4.1) — nunca hardcodeada.</li>
 *   <li>Llaman a {@link ValidadorBalanceAsiento#validar} antes de devolver
 *       nada: si no balancea, la excepción se propaga y el asiento NUNCA
 *       se persiste confirmado (regla de negocio de este paso).</li>
 *   <li>Piden el número a {@link NumeradorAsiento} una sola vez para todas
 *       las líneas del mismo asiento.</li>
 *   <li>Completan {@code documentoOrigenTipo}/{@code documentoOrigenId}
 *       para el vínculo bidireccional.</li>
 * </ol>
 * Todo cambio posterior al asiento generado (edición por rol permitido) se
 * audita igual que cualquier otra escritura sensible (F1.6).
 *
 * @param <T> tipo del evento de origen (p. ej. FacturaVenta, Cobro).
 */
public interface AsientoGenerator<T> {

    AsientoGenerado generar(T evento);
}
