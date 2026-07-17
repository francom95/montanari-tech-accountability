-- F3.5 — Anulación de asientos (F3.1 §4.4): motivo obligatorio de la marca
-- de anulación. El resto de la trazabilidad de anular (quién, cuándo) ya
-- queda en actualizado_en/actualizado_por (EntidadNegocio) y en el
-- auditoria_log (acción ANULAR); no se duplica con columnas propias.

ALTER TABLE asiento
    ADD COLUMN motivo_anulacion VARCHAR(500) AFTER observaciones;
