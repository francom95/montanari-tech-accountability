-- F1.8 — Moneda: entidad ejemplo del molde PL-1 (maestro simple, sin estados
-- ni relaciones todavía). codigo es el ISO 4217 (ARS, USD, ...).

CREATE TABLE moneda (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    codigo           VARCHAR(3)   NOT NULL,
    nombre           VARCHAR(80)  NOT NULL,
    simbolo          VARCHAR(5)   NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_moneda_tenant_codigo UNIQUE (tenant_id, codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO moneda (tenant_id, codigo, nombre, simbolo, activo, creado_en, creado_por, actualizado_en, actualizado_por, version)
VALUES
    (1, 'ARS', 'Peso Argentino', '$', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'USD', 'Dólar Estadounidense', 'US$', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
