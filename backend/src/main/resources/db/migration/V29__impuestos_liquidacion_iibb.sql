-- F6.2 — Liquidación mensual de IIBB (multi-jurisdicción, Convenio Multilateral).
-- Ninguna cuenta nueva: 5.3.2009 (Impuesto a los Ingresos Brutos), 2.1.2010
-- (IIBB a pagar) y 1.1.2008 (IIBB SIRCREB a favor) ya existían en el seed de
-- F3.3 sin mapeo, esperando este paso — igual que 2.1.2009 esperó para F6.1.
-- Solo hacen falta los mapeos concepto->cuenta que faltaban. Las deducciones
-- (percepciones, retenciones, SIRCREB, pagos a cuenta) reusan el mapeo de
-- PERCEPCION_IIBB_SUFRIDA -> 1.1.2008 que ya existe desde F4.3/V21.

INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'IMPUESTO_IIBB_DETERMINADO', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '5.3.2009'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'IIBB_A_PAGAR', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '2.1.2010'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'IIBB_SALDO_A_FAVOR', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2008'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- Cabecera de la liquidación de un período. Igual criterio que liquidacion_iva:
-- sin unique sobre (anio, mes) a propósito, para poder rehacer una anulada; la
-- unicidad real ("a lo sumo una viva por período") la valida el servicio.
CREATE TABLE liquidacion_iibb (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    anio                 INT           NOT NULL,
    mes                  INT           NOT NULL,
    fecha_desde          DATE          NOT NULL,
    fecha_hasta          DATE          NOT NULL,
    estado               VARCHAR(20)   NOT NULL,
    base_total           DECIMAL(18,2) NOT NULL DEFAULT 0,
    saldo_a_pagar_total  DECIMAL(18,2) NOT NULL DEFAULT 0,
    saldo_a_favor_total  DECIMAL(18,2) NOT NULL DEFAULT 0,
    asiento_id           BIGINT,
    observaciones        VARCHAR(2000),
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_liquidacion_iibb_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id),
    CONSTRAINT uk_liquidacion_iibb_asiento UNIQUE (asiento_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_liquidacion_iibb_periodo ON liquidacion_iibb (tenant_id, anio, mes, estado);

-- Sub-liquidación por jurisdicción. El coeficiente es el dato propio de Convenio
-- Multilateral (determinación anual CM05, no la calcula el sistema): editable,
-- con default = participación de la jurisdicción por destino. La alícuota es un
-- snapshot del maestro al crear, para que un cambio posterior en el maestro no
-- altere una liquidación ya confirmada. base_imponible = base_total * coeficiente;
-- impuesto_determinado = base_imponible * alicuota.
CREATE TABLE liquidacion_iibb_jurisdiccion (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id              BIGINT        NOT NULL,
    liquidacion_iibb_id    BIGINT        NOT NULL,
    jurisdiccion_id        BIGINT        NOT NULL,
    coeficiente            DECIMAL(9,6)  NOT NULL DEFAULT 0,
    base_imponible         DECIMAL(18,2) NOT NULL DEFAULT 0,
    alicuota               DECIMAL(5,2)  NOT NULL DEFAULT 0,
    impuesto_determinado   DECIMAL(18,2) NOT NULL DEFAULT 0,
    saldo_a_pagar          DECIMAL(18,2) NOT NULL DEFAULT 0,
    saldo_a_favor          DECIMAL(18,2) NOT NULL DEFAULT 0,
    orden                  INT           NOT NULL,
    creado_en              DATETIME(6)   NOT NULL,
    creado_por             VARCHAR(120),
    actualizado_en         DATETIME(6)   NOT NULL,
    actualizado_por        VARCHAR(120),
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_liquidacion_iibb_jur_liquidacion FOREIGN KEY (liquidacion_iibb_id) REFERENCES liquidacion_iibb(id),
    CONSTRAINT fk_liquidacion_iibb_jur_jurisdiccion FOREIGN KEY (jurisdiccion_id) REFERENCES jurisdiccion(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_liquidacion_iibb_jur ON liquidacion_iibb_jurisdiccion (liquidacion_iibb_id, orden);

-- Un renglón de deducción (o ajuste) dentro de una jurisdicción. El impuesto
-- determinado NO es un componente: es un campo de la jurisdicción (sale de
-- base * alícuota). Los componentes son las deducciones (percepciones,
-- retenciones, SIRCREB, pagos a cuenta), el arrastre y los ajustes manuales.
-- Mismo patrón auditable que liquidacion_iva_componente: importe_calculado
-- separado de importe_ajuste.
CREATE TABLE liquidacion_iibb_componente (
    id                            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                     BIGINT        NOT NULL,
    liquidacion_iibb_jur_id       BIGINT        NOT NULL,
    tipo                          VARCHAR(40)   NOT NULL,
    descripcion                   VARCHAR(300)  NOT NULL,
    importe_calculado             DECIMAL(18,2) NOT NULL DEFAULT 0,
    importe_ajuste                DECIMAL(18,2) NOT NULL DEFAULT 0,
    motivo_ajuste                 VARCHAR(500),
    cuenta_contable_id            BIGINT,
    manual                        BOOLEAN       NOT NULL DEFAULT FALSE,
    orden                         INT           NOT NULL,
    creado_en                     DATETIME(6)   NOT NULL,
    creado_por                    VARCHAR(120),
    actualizado_en                DATETIME(6)   NOT NULL,
    actualizado_por               VARCHAR(120),
    version                       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_liquidacion_iibb_comp_jur FOREIGN KEY (liquidacion_iibb_jur_id) REFERENCES liquidacion_iibb_jurisdiccion(id),
    CONSTRAINT fk_liquidacion_iibb_comp_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_liquidacion_iibb_comp ON liquidacion_iibb_componente (liquidacion_iibb_jur_id, orden);
