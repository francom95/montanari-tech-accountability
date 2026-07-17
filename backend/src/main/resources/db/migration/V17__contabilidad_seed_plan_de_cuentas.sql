-- F3.3 — Seed del plan de cuentas inicial.
-- Fuente: hoja 'Plan de Cuentas' del Excel 'Contabilidad 2026'. tenant_id = 1 (Montanari Tech).
-- Reglas (F3.1/F3.2): las cuentas madre solo agrupan (imputable=FALSE, sin rubro);
-- solo las imputables reciben movimientos y llevan rubro obligatorio. El saldo esperado
-- sigue la naturaleza: Activo y Resultado Negativo -> DEUDOR; Pasivo, Patrimonio Neto y
-- Resultado Positivo -> ACREEDOR. La madre '6 Otros Ingresos y Egresos' lleva la categoria
-- 'Otros Resultados' (OTROS_RESULTADOS); sus hijas se clasifican RP/RN segun su tipo y
-- conservan su saldo esperado real (deudor o acreedor).
--
-- Los codigos se transcriben tal cual la hoja (ej '1.1.2004'); Excel los mostraba como
-- fechas por su formato de celda, pero el codigo real se recupera del formato d.m.yyyy.
-- La rama '6' se saco de Resultado Negativo (era 5.4) a pedido del negocio, por contener
-- resultados positivos y negativos. La madre '3.1 Patrimonio Neto' se agrega (no esta en
-- el Excel) para dar el nivel intermedio que si tienen Activo (1.1) y Pasivo (2.1).

-- 1) Categorias contables (las 5 ramas / naturaleza).
INSERT INTO categoria_contable (tenant_id, nombre, descripcion, tipo, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'Activo', NULL, 'ACTIVO', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Pasivo', NULL, 'PASIVO', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Patrimonio Neto', NULL, 'PN', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Resultado Positivo', NULL, 'RP', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Resultado Negativo', NULL, 'RN', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Otros Resultados', NULL, 'OTROS_RESULTADOS', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- 2) Rubros (agrupadores dentro de cada categoria).
INSERT INTO rubro (tenant_id, nombre, categoria_id, orden, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '1. Caja y Bancos', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'ACTIVO'), 1, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2. Créditos por ventas', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'ACTIVO'), 2, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3. Otros Créditos por ventas', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'ACTIVO'), 3, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '4. Inversiones transitorias', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'ACTIVO'), 4, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1. Deudas Comerciales', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'PASIVO'), 5, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2. Deudas Sociales', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'PASIVO'), 6, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3. Deudas Fiscales', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'PASIVO'), 7, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '4. Deudas Bancarias', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'PASIVO'), 8, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5. Otras Deudas', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'PASIVO'), 9, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Patrimonio Neto', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'PN'), 10, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Ingresos por servicios brutos', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'RP'), 11, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Costos de prestacion por servicios', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'RN'), 12, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Gastos Operativos', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'RN'), 13, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'Otros Ingresos y Egresos', (SELECT id FROM categoria_contable WHERE tenant_id = 1 AND tipo = 'OTROS_RESULTADOS'), 14, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- 3) Cuentas contables (padre_id y rubro_id se resuelven en los pasos 4 y 5).
INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '1', 'ACTIVO', NULL, 'ACTIVO', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1', 'Activo Corriente', NULL, 'ACTIVO', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2001', 'Banco Galicia CC', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2002', 'Banco Galicia USD', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2003', 'Mercado Pago', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2004', 'Deudores por servicios prestados', NULL, 'ACTIVO', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2004.01', 'Deudores por servicios prestados - Valvecchia Gerardo', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2004.02', 'Deudores por servicios prestados - Centro de Ojos Quilmes S.A.', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2004.03', 'Deudores por servicios prestados - Kakaroto M4', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2005', 'Deudores por otros servicios prestados', NULL, 'ACTIVO', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2005.01', 'Deudores por otros servicios prestados - Moschen Prop.', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2005.02', 'Deudores por otros servicios prestados - Franco Lopez', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2005.03', 'Deudores por otros servicios prestados - Organica Real', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2006', 'IVA Crédito Fiscal', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2007', 'Percepcion de IVA a computar', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2008', 'IIBB SIRCREB a favor', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2009', 'IMP. CRE. LEY 25413 a favor', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2010', 'IMP. DEB LEY 25413 GRAL. a favor', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2011', 'Anticipo Imp. a las Ganancias', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2012', 'Inversiones Fondos Fima', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2', 'PASIVO', NULL, 'PASIVO', NULL, FALSE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1', 'Pasivo Corriente', NULL, 'PASIVO', NULL, FALSE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2001', 'Deudas por servicios prestados - Diseñadores', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2002', 'Deudas por servicios prestados - Programadores', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2003', 'Deudas por servicios prestados - Editores', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2004', 'Comisiones por Ventas a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2005', 'Sueldos a pagar - Franco Montanari', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2006', 'Contribuciones a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2007', 'Tarjeta VISA a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2008', 'IVA Débito Fiscal', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2009', 'IVA Saldo a Pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2010', 'IIBB a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2011', 'IG a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2012', 'Impuesto Bienes Personales - Acciones y Part. a Pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2013', 'Comision servicio de Cuenta Bco Galicia a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2014', 'Honorarios a contador a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2015', 'Intereses Financieros a devengar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2016', 'Intereses Financieros a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2017', 'Suscripciones a pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3', 'PATRIMONIO NETO', NULL, 'PN', NULL, FALSE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3.1', 'Patrimonio Neto', NULL, 'PN', NULL, FALSE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3.1.2001', 'Capital Social', NULL, 'PN', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '3.1.2002', 'Resultados Acumulados', NULL, 'PN', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '4', 'RESULTADO POSITIVO', NULL, 'RP', NULL, FALSE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '4.1', 'Ingresos Operativos', NULL, 'RP', NULL, FALSE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '4.1.2001', 'Ingresos por ventas', NULL, 'RP', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '4.1.2002', 'Ingresos por otras ventas', NULL, 'RP', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5', 'RESULTADO NEGATIVO', NULL, 'RN', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.1', 'Costos de prestacion por servicios', NULL, 'RN', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.1.2001', 'Costo Diseñador', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.1.2002', 'Costo Programador', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.1.2003', 'Costo Editores', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.1.2004', 'Costo de licencias de Software - suscripciones', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.2', 'Gastos de Comercialización', NULL, 'RN', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.2.2001', 'Comisión por Ventas', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3', 'Gastos de Administración y Financieros', NULL, 'RN', NULL, FALSE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2001', 'TRANSF AFIP VEP (Gasto / Impuesto)', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2002', 'Honorarios Profesionales', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2003', 'Sueldos - Franco Montanari', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2004', 'Contribuciones Patronales', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2005', 'Intereses Resarcitorios', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2006', 'Intereses Financieros', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2007', 'Impuesto a las ganancias', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2008', 'Impuesto Bienes Personales - Acciones y Part', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2009', 'Impuesto a los Ingresos Brutos', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2010', 'Impuesto sobre sellos', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '5.3.2011', 'Suscripciones', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '6', 'Otros Ingresos y Egresos', NULL, 'OTROS_RESULTADOS', NULL, FALSE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '6.4001', 'Comisiones Ganadas', NULL, 'RP', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '6.4002', 'Intereses Ganados', NULL, 'RP', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '6.4003', 'Comisiones bancarias', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '6.4004', 'Comisiones Mercado Pago', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- 4) Asignar rubro a las cuentas imputables (las madre quedan sin rubro).
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '1. Caja y Bancos')
    WHERE tenant_id = 1 AND codigo IN ('1.1.2001', '1.1.2002', '1.1.2003');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '2. Créditos por ventas')
    WHERE tenant_id = 1 AND codigo IN ('1.1.2004.01', '1.1.2004.02', '1.1.2004.03', '1.1.2005.01', '1.1.2005.02', '1.1.2005.03');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '3. Otros Créditos por ventas')
    WHERE tenant_id = 1 AND codigo IN ('1.1.2006', '1.1.2007', '1.1.2008', '1.1.2009', '1.1.2010', '1.1.2011');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '4. Inversiones transitorias')
    WHERE tenant_id = 1 AND codigo IN ('1.1.2012');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '1. Deudas Comerciales')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2001', '2.1.2002', '2.1.2003', '2.1.2004');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '2. Deudas Sociales')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2005', '2.1.2006');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '3. Deudas Fiscales')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2007', '2.1.2008', '2.1.2009', '2.1.2010', '2.1.2011', '2.1.2012');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '4. Deudas Bancarias')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2013');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '5. Otras Deudas')
    WHERE tenant_id = 1 AND codigo IN ('2.1.2014', '2.1.2015', '2.1.2016', '2.1.2017');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Patrimonio Neto')
    WHERE tenant_id = 1 AND codigo IN ('3.1.2001', '3.1.2002');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Ingresos por servicios brutos')
    WHERE tenant_id = 1 AND codigo IN ('4.1.2001', '4.1.2002');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Costos de prestacion por servicios')
    WHERE tenant_id = 1 AND codigo IN ('5.1.2001', '5.1.2002', '5.1.2003', '5.1.2004');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Gastos Operativos')
    WHERE tenant_id = 1 AND codigo IN ('5.2.2001', '5.3.2001', '5.3.2002', '5.3.2003', '5.3.2004', '5.3.2005', '5.3.2006', '5.3.2007', '5.3.2008', '5.3.2009', '5.3.2010', '5.3.2011');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Otros Ingresos y Egresos')
    WHERE tenant_id = 1 AND codigo IN ('6.4001', '6.4002', '6.4003', '6.4004');

-- 5) Enlazar cada cuenta con su madre (ancestro existente mas cercano).
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('1.1');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '2'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('2.1');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '3'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('3.1');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '4'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('4.1');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '5'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('5.1', '5.2', '5.3');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '6'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('6.4001', '6.4002', '6.4003', '6.4004');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('1.1.2001', '1.1.2002', '1.1.2003', '1.1.2004', '1.1.2005', '1.1.2006', '1.1.2007', '1.1.2008', '1.1.2009', '1.1.2010', '1.1.2011', '1.1.2012');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '2.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('2.1.2001', '2.1.2002', '2.1.2003', '2.1.2004', '2.1.2005', '2.1.2006', '2.1.2007', '2.1.2008', '2.1.2009', '2.1.2010', '2.1.2011', '2.1.2012', '2.1.2013', '2.1.2014', '2.1.2015', '2.1.2016', '2.1.2017');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '3.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('3.1.2001', '3.1.2002');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '4.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('4.1.2001', '4.1.2002');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '5.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('5.1.2001', '5.1.2002', '5.1.2003', '5.1.2004');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '5.2'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('5.2.2001');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '5.3'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('5.3.2001', '5.3.2002', '5.3.2003', '5.3.2004', '5.3.2005', '5.3.2006', '5.3.2007', '5.3.2008', '5.3.2009', '5.3.2010', '5.3.2011');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1.1.2004'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('1.1.2004.01', '1.1.2004.02', '1.1.2004.03');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1.1.2005'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('1.1.2005.01', '1.1.2005.02', '1.1.2005.03');

