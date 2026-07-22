-- F5.3 — Conciliación bancaria: imputación rápida (un clic) para movimientos
-- bancarios sin match contra un asiento existente. SIRCREB y percepciones
-- bancarias NO suman cuentas/conceptos nuevos: reusan PERCEPCION_IIBB_SUFRIDA
-- (1.1.2008) y PERCEPCION_IVA_SUFRIDA (1.1.2007) ya mapeadas en F4.3 — es la
-- misma percepción "sufrida", solo que originada en un extracto bancario en
-- vez de una factura. Acá solo hacen falta 2 cuentas realmente nuevas:
-- comisiones bancarias (ya existía, 6.4003/6.4004 de F1.8) e impuesto Ley
-- 25413 (débitos y créditos bancarios), que no tenía cuenta propia todavía.

INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '5.3.2012', 'Impuesto Ley 25413 (débitos y créditos bancarios)', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Gastos Operativos')
    WHERE tenant_id = 1 AND codigo = '5.3.2012';

UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '5.3'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo = '5.3.2012';

-- Mapeo concepto->cuenta (F4.1 §1). COMISION_BANCARIA tiene una fila por
-- defecto (6.4003, cualquier origen) y una específica para Mercado Pago
-- (6.4004, ya existía desde F1.8 con ese propósito puntual) — discriminador
-- por origen de importación (F5.2), no por cuenta bancaria: la comisión de
-- MP siempre va a la misma cuenta sea cual sea la cuenta bancaria destino.
INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'COMISION_BANCARIA', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '6.4003'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'COMISION_BANCARIA', 'ORIGEN_IMPORTACION', 'MERCADO_PAGO', (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '6.4004'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'IMPUESTO_DEBITOS_CREDITOS_BANCARIOS', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '5.3.2012'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
