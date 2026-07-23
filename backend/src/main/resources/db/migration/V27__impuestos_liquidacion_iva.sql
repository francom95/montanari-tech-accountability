-- F6.1 — Liquidación mensual de IVA. La cuenta 2.1.2009 'IVA Saldo a Pagar'
-- ya existía desde el seed (V17) sin mapeo ni uso: era justamente la que
-- esperaba este paso. Falta su contracara del lado activo — el saldo a favor
-- que se arrastra al período siguiente — que no estaba en el seed.
--
-- Un solo acumulador de saldo a favor (técnico), no dos: el usuario indicó
-- que el saldo a favor "se arrastra al mes siguiente" sin distinguir saldo
-- técnico de saldo de libre disponibilidad. La normativa argentina sí los
-- distingue (art. 24 Ley 23.349) y Montanari Tech sufre percepciones
-- habitualmente, así que el punto queda documentado para el checkpoint del
-- contador — separarlos después obliga a recalcular los períodos históricos.

INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '1.1.2014', 'IVA Saldo a Favor', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '3. Otros Créditos por ventas')
    WHERE tenant_id = 1 AND codigo = '1.1.2014';

UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo = '1.1.2014';

-- Mapeo concepto->cuenta (F4.1 §1). Los otros tres conceptos que usa la
-- liquidación (IVA_DEBITO_FISCAL, IVA_CREDITO_FISCAL, PERCEPCION_IVA_SUFRIDA)
-- ya están mapeados desde V20/V21 — el motor de IVA los lee de ahí en vez de
-- hardcodear códigos de cuenta, así que si el usuario reasigna un mapeo la
-- liquidación lo sigue solo.
INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'IVA_SALDO_A_PAGAR', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '2.1.2009'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'IVA_SALDO_A_FAVOR', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2014'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- Cabecera de la liquidación de un período (año+mes), molde PL-5
-- (borrador/confirmado/anulado, one-way). Sin unique constraint sobre
-- (tenant_id, anio, mes) a propósito: una liquidación anulada debe poder
-- rehacerse creando otra del mismo período. La unicidad real ("a lo sumo una
-- liquidación viva por período") la valida el servicio contra los estados
-- BORRADOR y CONFIRMADO, cosa que un índice único no puede expresar en MySQL.
CREATE TABLE liquidacion_iva (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    anio                 INT           NOT NULL,
    mes                  INT           NOT NULL,
    fecha_desde          DATE          NOT NULL,
    fecha_hasta          DATE          NOT NULL,
    estado               VARCHAR(20)   NOT NULL,
    saldo_a_pagar        DECIMAL(18,2) NOT NULL DEFAULT 0,
    saldo_a_favor        DECIMAL(18,2) NOT NULL DEFAULT 0,
    asiento_id           BIGINT,
    observaciones        VARCHAR(2000),
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_liquidacion_iva_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id),
    CONSTRAINT uk_liquidacion_iva_asiento UNIQUE (asiento_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_liquidacion_iva_periodo ON liquidacion_iva (tenant_id, anio, mes, estado);

-- Un renglón por componente. Tabla hija en vez de columnas anchas en la
-- cabecera porque el plan pide poder "agregar o corregir conceptos": los
-- automáticos (débito fiscal, crédito fiscal, percepciones, arrastre) se
-- recalculan desde los asientos, y el usuario puede sumar filas manuales
-- (restituciones u otros) que traen su propia cuenta contable — sin ella el
-- asiento de confirmación no podría balancear.
--
-- importe_calculado se guarda separado de importe_ajuste justamente para que
-- el ajuste sea auditable: queda registrado qué dijo el sistema y qué decidió
-- la persona, no solo el resultado final.
CREATE TABLE liquidacion_iva_componente (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    liquidacion_iva_id   BIGINT        NOT NULL,
    tipo                 VARCHAR(40)   NOT NULL,
    descripcion          VARCHAR(300)  NOT NULL,
    importe_calculado    DECIMAL(18,2) NOT NULL DEFAULT 0,
    importe_ajuste       DECIMAL(18,2) NOT NULL DEFAULT 0,
    motivo_ajuste        VARCHAR(500),
    cuenta_contable_id   BIGINT,
    manual               BOOLEAN       NOT NULL DEFAULT FALSE,
    orden                INT           NOT NULL,
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_liquidacion_iva_componente_liquidacion FOREIGN KEY (liquidacion_iva_id) REFERENCES liquidacion_iva(id),
    CONSTRAINT fk_liquidacion_iva_componente_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_liquidacion_iva_componente_liquidacion ON liquidacion_iva_componente (liquidacion_iva_id, orden);
