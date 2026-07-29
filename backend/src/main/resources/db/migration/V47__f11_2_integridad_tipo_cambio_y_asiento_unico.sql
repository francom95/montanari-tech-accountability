-- F11.2 A1: nada validaba que tipoCambio fuera positivo — un TC=0 o negativo
-- corrompía en silencio el totalArs (verificado en vivo en F11.1). asiento_linea
-- ya tenía este CHECK (chk_asiento_linea_tc); estas 8 tablas no.
ALTER TABLE atribucion_impuesto ADD CONSTRAINT chk_atribucion_impuesto_tc CHECK (tipo_cambio > 0);
ALTER TABLE cobro ADD CONSTRAINT chk_cobro_tc CHECK (tipo_cambio > 0);
ALTER TABLE consumo_tarjeta ADD CONSTRAINT chk_consumo_tarjeta_tc CHECK (tipo_cambio > 0);
ALTER TABLE factura_compra ADD CONSTRAINT chk_factura_compra_tc CHECK (tipo_cambio > 0);
ALTER TABLE factura_venta ADD CONSTRAINT chk_factura_venta_tc CHECK (tipo_cambio > 0);
ALTER TABLE movimiento_bancario ADD CONSTRAINT chk_movimiento_bancario_tc CHECK (tipo_cambio > 0);
ALTER TABLE pago ADD CONSTRAINT chk_pago_tc CHECK (tipo_cambio > 0);
ALTER TABLE pago_tarjeta ADD CONSTRAINT chk_pago_tarjeta_tc CHECK (tipo_cambio > 0);

-- F11.2 A10: un mismo asiento podía vincularse a N documentos vía
-- confirmarVinculandoAsientoExistente (F10.3), sin ninguna restricción que lo
-- impidiera. UNIQUE con NULLs (MySQL permite múltiples NULL bajo un índice
-- único) no afecta a los documentos con asiento generado automáticamente.
ALTER TABLE factura_venta ADD CONSTRAINT uk_factura_venta_asiento UNIQUE (asiento_id);
ALTER TABLE factura_compra ADD CONSTRAINT uk_factura_compra_asiento UNIQUE (asiento_id);
ALTER TABLE cobro ADD CONSTRAINT uk_cobro_asiento UNIQUE (asiento_id);
ALTER TABLE pago ADD CONSTRAINT uk_pago_asiento UNIQUE (asiento_id);
