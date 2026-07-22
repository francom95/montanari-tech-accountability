-- F5.2 — Parsers de resúmenes (Galicia, Mercado Pago, tarjeta): los movimientos
-- importados entran igual que los manuales (PENDIENTE, F5.1), pero algunos
-- orígenes no traen fecha en todas las filas (ej. Galicia ARS.xlsx no trae
-- fecha en ninguna fila real) — el usuario la completa a mano en la bandeja
-- ("corregir") antes de poder confirmar/imputar. Por eso fecha pasa a nullable.

ALTER TABLE movimiento_bancario MODIFY COLUMN fecha DATE NULL;

-- Detección de duplicados al re-importar (hash de fecha+importe+descripción+
-- cuenta, ver plan F5.2): único por cuenta bancaria, nulo para carga manual.
ALTER TABLE movimiento_bancario ADD COLUMN hash_importacion VARCHAR(64) NULL AFTER origen_importacion;
CREATE UNIQUE INDEX uk_movimiento_bancario_hash ON movimiento_bancario (cuenta_bancaria_id, hash_importacion);
