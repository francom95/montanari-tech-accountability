package com.montanaritech.contable.common.asiento;

/**
 * Molde de referencia de PL-4 (F1.8): un generador por tipo de evento
 * confirmado que impacta contabilidad (factura de venta, factura de
 * compra, cobro, pago, liquidación de IVA/IIBB, resumen de tarjeta —
 * F3.x/F4.x/F6.x). Implementaciones:
 * <ol>
 *   <li>Reciben el evento a punto de confirmarse (nunca un borrador).</li>
 *   <li>Arman las líneas debe/haber resolviendo cada cuenta vía
 *       {@code ResolutorCuentas} sobre la tabla de mapeo concepto→cuenta
 *       (editable por admin, F4.1) — nunca hardcodeada.</li>
 *   <li>Completan {@code documentoOrigenTipo}/{@code documentoOrigenId}
 *       para el vínculo bidireccional.</li>
 *   <li><b>No</b> piden número ni validan balance ellas mismas: entregan el
 *       {@link AsientoGenerado} a {@code AsientoService.registrarAutomatico},
 *       que corre el mismo checklist que la carga manual (balance,
 *       XOR debe/haber, cuenta imputable/activa, consistencia ARS) y recién
 *       al final asigna el número — si algo falla, nada se persiste
 *       (F3.1 §3.4, ADR-07: {@code AsientoService} es el único punto de
 *       escritura de {@code asiento}/{@code asiento_linea}).</li>
 * </ol>
 * Todo cambio posterior al asiento generado (edición por rol permitido) se
 * audita igual que cualquier otra escritura sensible (F1.6).
 *
 * @param <T> tipo del evento de origen (p. ej. FacturaVenta, Cobro).
 */
public interface AsientoGenerator<T> {

    AsientoGenerado generar(T evento);
}
