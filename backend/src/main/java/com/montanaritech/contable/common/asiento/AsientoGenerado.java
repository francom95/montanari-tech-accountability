package com.montanaritech.contable.common.asiento;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de un {@link AsientoGenerator}, listo para
 * {@code AsientoService.registrarAutomatico} (F3.1 §8.1, F4.1 §9): la cabecera
 * completa + sus líneas + el vínculo al documento origen. {@code origen} es
 * el nombre del enum {@code OrigenAsiento} (p. ej. {@code "FACTURA_VENTA"})
 * como String, no el enum en sí — evita que este paquete común dependa de
 * {@code contabilidad.asiento}; {@code AsientoService} lo convierte al
 * persistir.
 *
 * <p>A diferencia del molde original de F1.8, <b>no</b> lleva un número ya
 * asignado: {@code registrarAutomatico} lo pide al final, después de validar
 * balance (mismo orden que {@code AsientoService.confirmar}, F3.1 §3.4 ítem
 * 6) — pedirlo antes dejaría huecos de numeración si la validación fallara.
 */
public record AsientoGenerado(
        LocalDate fecha,
        String descripcion,
        String origen,
        List<LineaAsientoGenerada> lineas,
        String documentoOrigenTipo,
        Long documentoOrigenId
) {
}
