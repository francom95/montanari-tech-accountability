-- F10.3 — Cuentas contables faltantes para reflejar el asiento de apertura
-- (EECC.pdf, balance real cerrado al 31/08/2025 de "ST Surfing Reservations SRL").
-- Nota 1 del EECC desglosa cada rubro por cuenta individual; estas 11 cuentas
-- son las que no tenían equivalente en el plan de cuentas sembrado por F3.3.
INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '1.1.2016', 'Caja', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2017', 'Banco Nación', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2018', 'Banco Provincia', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2019', 'Retenciones de Ganancias sufridas', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2018', 'Proveedores', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2019', 'Seguro de Vida Obligatorio a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2020', 'ART a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2021', 'Honorarios Directores a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2022', 'Plan de Facilidades a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3.1.2003', 'Ajuste al Capital', NULL, 'PN', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3.1.2004', 'Ajuste Ejercicios Anteriores', NULL, 'PN', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- Rubro (mismos rubros ya sembrados por F3.3, ver V17).
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '1. Caja y Bancos')
    WHERE tenant_id = 1 AND codigo IN ('1.1.2016', '1.1.2017', '1.1.2018');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '3. Otros Créditos por ventas')
    WHERE tenant_id = 1 AND codigo IN ('1.1.2019');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '1. Deudas Comerciales')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2018');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '2. Deudas Sociales')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2019', '2.1.2020', '2.1.2021');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '3. Deudas Fiscales')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2022');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Patrimonio Neto')
    WHERE tenant_id = 1 AND codigo IN ('3.1.2003', '3.1.2004');

-- padre_id: mismas cuentas madre '1.1'/'2.1'/'3.1' ya sembradas por F3.3.
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('1.1.2016', '1.1.2017', '1.1.2018', '1.1.2019');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '2.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('2.1.2018', '2.1.2019', '2.1.2020', '2.1.2021', '2.1.2022');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '3.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('3.1.2003', '3.1.2004');
