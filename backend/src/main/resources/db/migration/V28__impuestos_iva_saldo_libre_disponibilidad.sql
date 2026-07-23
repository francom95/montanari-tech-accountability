-- F6.1 (corrección post-checkpoint del contador). Dos cambios que vienen de la
-- validación humana del paso:
--
-- 1) Separar el saldo a favor en sus dos especies del art. 24 de la Ley 23.349.
--    El SALDO TÉCNICO (primer párrafo: crédito fiscal > débito fiscal) solo se
--    aplica contra débitos fiscales de períodos siguientes. El SALDO DE LIBRE
--    DISPONIBILIDAD (segundo párrafo: ingresos directos —percepciones y
--    retenciones sufridas— que exceden el impuesto determinado) además se
--    compensa con otros impuestos, se transfiere y se puede pedir devuelto.
--    Mezclarlos en un solo acumulador daba el mismo total pero componía mal el
--    saldo, y Montanari Tech sufre percepciones habitualmente.
--
-- 2) Ver V28 en el motor: el IVA de una nota de crédito emitida no reduce el
--    débito fiscal sino que aumenta el crédito fiscal (art. 12 inc. b), y el de
--    una nota de crédito recibida aumenta el débito fiscal (art. 11 último
--    párrafo). Eso no necesita cuentas nuevas —se distingue por el lado de la
--    imputación— pero sí cambia la composición que esta migración habilita.

INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '1.1.2015', 'IVA Saldo de Libre Disponibilidad', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '3. Otros Créditos por ventas')
    WHERE tenant_id = 1 AND codigo = '1.1.2015';

UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo = '1.1.2015';

INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'IVA_SALDO_LIBRE_DISPONIBILIDAD', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2015'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- El saldo a favor deja de ser uno solo. La columna vieja pasa a significar
-- exclusivamente "saldo técnico"; la nueva guarda la libre disponibilidad.
-- Sin backfill: las liquidaciones existentes son de la verificación E2E, y
-- recalcular su composición requeriría re-liquidar el período de todos modos.
ALTER TABLE liquidacion_iva ADD COLUMN saldo_libre_disponibilidad DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER saldo_a_favor;
