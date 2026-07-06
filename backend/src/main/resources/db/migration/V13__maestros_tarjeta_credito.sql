-- F2.4 — Tarjeta de crédito: entidad, moneda (FK), día de cierre/vencimiento,
-- cuenta bancaria de débito asociada (FK), saldo inicial con fecha (recalculable), activo.

CREATE TABLE tarjeta_credito (
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                  BIGINT        NOT NULL,
    entidad                    VARCHAR(80)   NOT NULL,
    moneda_id                  BIGINT        NOT NULL,
    dia_cierre                 INT           NOT NULL,
    dia_vencimiento            INT           NOT NULL,
    cuenta_bancaria_debito_id  BIGINT        NOT NULL,
    saldo_inicial              DECIMAL(18,2) NOT NULL,
    fecha_saldo_inicial        DATE          NOT NULL,
    saldo_actual               DECIMAL(18,2) NOT NULL,
    activo                     BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en                  DATETIME(6)   NOT NULL,
    creado_por                 VARCHAR(120),
    actualizado_en             DATETIME(6)   NOT NULL,
    actualizado_por            VARCHAR(120),
    version                    BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_tarjeta_credito_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_tarjeta_credito_cuenta_debito FOREIGN KEY (cuenta_bancaria_debito_id) REFERENCES cuenta_bancaria(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
