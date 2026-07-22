-- F4.4 — Cobros y pagos (F4.1 §6). Agrega la cuenta contable espejo de
-- cuenta_bancaria (F3.1 §2.3, F4.1 §2.3 — F2.4 nunca la wireó pese a que el
-- diseño ya la daba por existente) y las tablas de cobro/pago con sus
-- imputaciones y aplicaciones de anticipo.

-- 1) Cuenta contable espejo 1:1 de cuenta_bancaria. Backfill de las 3 cuentas
-- bancarias sembradas en F2.4 contra sus cuentas homónimas del plan de
-- cuentas (F3.3): Banco Galicia CC->1.1.2001, Banco Galicia USD->1.1.2002,
-- Mercado Pago->1.1.2003.
ALTER TABLE cuenta_bancaria ADD COLUMN cuenta_contable_id BIGINT NULL AFTER tipo;

UPDATE cuenta_bancaria SET cuenta_contable_id = (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2001')
    WHERE tenant_id = 1 AND alias = 'Banco Galicia CC';
UPDATE cuenta_bancaria SET cuenta_contable_id = (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2002')
    WHERE tenant_id = 1 AND alias = 'Banco Galicia USD';
UPDATE cuenta_bancaria SET cuenta_contable_id = (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2003')
    WHERE tenant_id = 1 AND alias = 'Mercado Pago';

ALTER TABLE cuenta_bancaria MODIFY COLUMN cuenta_contable_id BIGINT NOT NULL;
ALTER TABLE cuenta_bancaria ADD CONSTRAINT fk_cuenta_bancaria_cuenta_contable FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id);

-- 1.1) Mapeo concepto->cuenta (F4.1 §1) para los 4 conceptos creados en V20
-- (checkpoint F4.1 §3) que ningún generador anterior consumía todavía:
-- son cuentas globales fijas (sin discriminador), mismo criterio que
-- IVA_DEBITO_FISCAL/IVA_CREDITO_FISCAL en V20/V21.
INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'DIF_CAMBIO_GANADA', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '6.4005'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'DIF_CAMBIO_PERDIDA', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '6.4006'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'ANTICIPO_CLIENTE', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '2.1.2018'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'ANTICIPO_PROVEEDOR', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2013'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- 2) Cobro: cabecera + imputaciones a facturas de venta + aplicaciones
-- posteriores de anticipo (F4.1 §6.1/§6.5). `monto_anticipo` es el
-- remanente no imputado (total - Σ imputaciones), congelado al confirmar;
-- se va consumiendo vía aplicacion_anticipo_cliente sin reabrir el asiento
-- original del cobro (F3.1 §6.5, D-4: el asiento del cobro jamás se edita).
CREATE TABLE cobro (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    cliente_id            BIGINT        NOT NULL,
    fecha                 DATE          NOT NULL,
    moneda_id             BIGINT        NOT NULL,
    tipo_cambio           DECIMAL(19,6) NOT NULL,
    fuente_tc             VARCHAR(20),
    cuenta_bancaria_id    BIGINT        NOT NULL,
    total                 DECIMAL(18,2) NOT NULL,
    total_ars             DECIMAL(18,2) NOT NULL,
    importe_retenciones   DECIMAL(18,2) NOT NULL DEFAULT 0,
    monto_anticipo        DECIMAL(18,2) NOT NULL DEFAULT 0,
    estado                VARCHAR(20)   NOT NULL,
    asiento_id            BIGINT,
    observaciones         VARCHAR(2000),
    creado_en             DATETIME(6)   NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)   NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_cobro_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_cobro_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_cobro_cuenta_bancaria FOREIGN KEY (cuenta_bancaria_id) REFERENCES cuenta_bancaria(id),
    CONSTRAINT fk_cobro_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_cobro_tenant_fecha ON cobro (tenant_id, fecha);
CREATE INDEX ix_cobro_cliente ON cobro (cliente_id);

CREATE TABLE cobro_imputacion (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                 BIGINT        NOT NULL,
    cobro_id                  BIGINT        NOT NULL,
    factura_venta_id          BIGINT        NOT NULL,
    orden                     INT           NOT NULL,
    monto_imputado_original   DECIMAL(18,2) NOT NULL,
    monto_ars_cancelado       DECIMAL(18,2),
    creado_en                 DATETIME(6)   NOT NULL,
    creado_por                VARCHAR(120),
    actualizado_en            DATETIME(6)   NOT NULL,
    actualizado_por           VARCHAR(120),
    version                   BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_cobro_imputacion_cobro FOREIGN KEY (cobro_id) REFERENCES cobro(id) ON DELETE CASCADE,
    CONSTRAINT fk_cobro_imputacion_factura FOREIGN KEY (factura_venta_id) REFERENCES factura_venta(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_cobro_imputacion_cobro ON cobro_imputacion (cobro_id);
CREATE INDEX ix_cobro_imputacion_factura ON cobro_imputacion (factura_venta_id);

-- Aplicación posterior de un anticipo (F4.1 §6.5, CO-5): registro append-only
-- (no hay borrador/estado propio), cada fila referencia el asiento AJUSTE
-- que generó.
CREATE TABLE aplicacion_anticipo_cliente (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    cobro_id              BIGINT        NOT NULL,
    factura_venta_id      BIGINT        NOT NULL,
    fecha                 DATE          NOT NULL,
    monto_original        DECIMAL(18,2) NOT NULL,
    monto_ars_cancelado   DECIMAL(18,2) NOT NULL,
    asiento_id            BIGINT        NOT NULL,
    creado_en             DATETIME(6)   NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)   NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_aplicacion_anticipo_cliente_cobro FOREIGN KEY (cobro_id) REFERENCES cobro(id),
    CONSTRAINT fk_aplicacion_anticipo_cliente_factura FOREIGN KEY (factura_venta_id) REFERENCES factura_venta(id),
    CONSTRAINT fk_aplicacion_anticipo_cliente_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_aplicacion_anticipo_cliente_cobro ON aplicacion_anticipo_cliente (cobro_id);
CREATE INDEX ix_aplicacion_anticipo_cliente_factura ON aplicacion_anticipo_cliente (factura_venta_id);

-- 3) Pago: simétrico a cobro, sin retenciones (Montanari no es agente de
-- retención, checkpoint F4.1 #3 — ver [[f43-facturas-compra-decisiones]]).
CREATE TABLE pago (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    proveedor_id          BIGINT        NOT NULL,
    fecha                 DATE          NOT NULL,
    moneda_id             BIGINT        NOT NULL,
    tipo_cambio           DECIMAL(19,6) NOT NULL,
    fuente_tc             VARCHAR(20),
    cuenta_bancaria_id    BIGINT        NOT NULL,
    total                 DECIMAL(18,2) NOT NULL,
    total_ars             DECIMAL(18,2) NOT NULL,
    monto_anticipo        DECIMAL(18,2) NOT NULL DEFAULT 0,
    estado                VARCHAR(20)   NOT NULL,
    asiento_id            BIGINT,
    observaciones         VARCHAR(2000),
    creado_en             DATETIME(6)   NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)   NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_pago_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_pago_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_pago_cuenta_bancaria FOREIGN KEY (cuenta_bancaria_id) REFERENCES cuenta_bancaria(id),
    CONSTRAINT fk_pago_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_pago_tenant_fecha ON pago (tenant_id, fecha);
CREATE INDEX ix_pago_proveedor ON pago (proveedor_id);

CREATE TABLE pago_imputacion (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                 BIGINT        NOT NULL,
    pago_id                   BIGINT        NOT NULL,
    factura_compra_id         BIGINT        NOT NULL,
    orden                     INT           NOT NULL,
    monto_imputado_original   DECIMAL(18,2) NOT NULL,
    monto_ars_cancelado       DECIMAL(18,2),
    creado_en                 DATETIME(6)   NOT NULL,
    creado_por                VARCHAR(120),
    actualizado_en            DATETIME(6)   NOT NULL,
    actualizado_por           VARCHAR(120),
    version                   BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_pago_imputacion_pago FOREIGN KEY (pago_id) REFERENCES pago(id) ON DELETE CASCADE,
    CONSTRAINT fk_pago_imputacion_factura FOREIGN KEY (factura_compra_id) REFERENCES factura_compra(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_pago_imputacion_pago ON pago_imputacion (pago_id);
CREATE INDEX ix_pago_imputacion_factura ON pago_imputacion (factura_compra_id);

CREATE TABLE aplicacion_anticipo_proveedor (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    pago_id               BIGINT        NOT NULL,
    factura_compra_id     BIGINT        NOT NULL,
    fecha                 DATE          NOT NULL,
    monto_original        DECIMAL(18,2) NOT NULL,
    monto_ars_cancelado   DECIMAL(18,2) NOT NULL,
    asiento_id            BIGINT        NOT NULL,
    creado_en             DATETIME(6)   NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)   NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_aplicacion_anticipo_proveedor_pago FOREIGN KEY (pago_id) REFERENCES pago(id),
    CONSTRAINT fk_aplicacion_anticipo_proveedor_factura FOREIGN KEY (factura_compra_id) REFERENCES factura_compra(id),
    CONSTRAINT fk_aplicacion_anticipo_proveedor_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_aplicacion_anticipo_proveedor_pago ON aplicacion_anticipo_proveedor (pago_id);
CREATE INDEX ix_aplicacion_anticipo_proveedor_factura ON aplicacion_anticipo_proveedor (factura_compra_id);
