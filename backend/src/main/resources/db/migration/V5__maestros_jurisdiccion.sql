-- F2.1 — Jurisdicción: nombre, código, alícuota IIBB, activo.

CREATE TABLE jurisdiccion (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    nombre           VARCHAR(80)  NOT NULL,
    codigo           VARCHAR(20)  NOT NULL,
    alicuota_iibb    DECIMAL(5,2) NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_jurisdiccion_tenant_codigo UNIQUE (tenant_id, codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
