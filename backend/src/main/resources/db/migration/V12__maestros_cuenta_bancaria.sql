-- F2.4 — Cuenta bancaria / cuenta de dinero: entidad, alias, moneda (FK), tipo,
-- estado de conciliación, saldo inicial con fecha (recalculable), activo.

CREATE TABLE cuenta_bancaria (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    entidad              VARCHAR(80)   NOT NULL,
    alias                VARCHAR(80)   NOT NULL,
    moneda_id            BIGINT        NOT NULL,
    tipo                 VARCHAR(20)   NOT NULL,
    estado_conciliacion  VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    saldo_inicial        DECIMAL(18,2) NOT NULL,
    fecha_saldo_inicial  DATE          NOT NULL,
    saldo_actual         DECIMAL(18,2) NOT NULL,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_cuenta_bancaria_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT uk_cuenta_bancaria_tenant_alias UNIQUE (tenant_id, alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed pedido explícitamente por la especificación de F2.4.
INSERT INTO cuenta_bancaria (tenant_id, entidad, alias, moneda_id, tipo, estado_conciliacion, saldo_inicial, fecha_saldo_inicial, saldo_actual, activo, creado_en, creado_por, actualizado_en, actualizado_por, version)
SELECT 1, 'Banco Galicia', 'Banco Galicia CC', id, 'CUENTA_CORRIENTE', 'PENDIENTE', 0, UTC_DATE(), 0, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0
FROM moneda WHERE tenant_id = 1 AND codigo = 'ARS'
UNION ALL
SELECT 1, 'Banco Galicia', 'Banco Galicia USD', id, 'CAJA_AHORRO', 'PENDIENTE', 0, UTC_DATE(), 0, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0
FROM moneda WHERE tenant_id = 1 AND codigo = 'USD'
UNION ALL
SELECT 1, 'Mercado Pago', 'Mercado Pago', id, 'MERCADO_PAGO', 'PENDIENTE', 0, UTC_DATE(), 0, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0
FROM moneda WHERE tenant_id = 1 AND codigo = 'ARS';
