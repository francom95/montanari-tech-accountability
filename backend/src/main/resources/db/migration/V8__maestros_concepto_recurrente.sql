-- F2.1 — Concepto recurrente: nombre, descripción, categoría/cuenta sugerida, periodicidad, importe, moneda, activo.

CREATE TABLE concepto_recurrente (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    nombre           VARCHAR(80)  NOT NULL,
    descripcion      VARCHAR(500),
    cuenta_sugerida  VARCHAR(20),
    periodicidad     VARCHAR(50),
    importe          DECIMAL(19,2),
    moneda_id        BIGINT,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_concepto_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT uk_concepto_tenant_nombre UNIQUE (tenant_id, nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
