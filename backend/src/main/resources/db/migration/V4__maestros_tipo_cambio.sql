-- F2.1 — TipoCambio: fecha, moneda, criterio, valor_compra, valor_venta, fuente, observaciones, activo.

CREATE TABLE tipo_cambio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    fecha            DATE         NOT NULL,
    moneda_id        BIGINT       NOT NULL,
    criterio         VARCHAR(50)  NOT NULL,
    valor_compra     DECIMAL(19,4) NOT NULL,
    valor_venta      DECIMAL(19,4) NOT NULL,
    fuente           VARCHAR(120),
    observaciones    VARCHAR(500),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_tipo_cambio_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT uk_tipo_cambio_tenant_fecha_moneda_criterio UNIQUE (tenant_id, fecha, moneda_id, criterio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
