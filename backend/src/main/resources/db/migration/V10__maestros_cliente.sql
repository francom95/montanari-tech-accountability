-- F2.2 — Cliente: nombre, CUIT, jurisdicción, contacto, activo.

CREATE TABLE cliente (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    nombre           VARCHAR(120) NOT NULL,
    cuit             VARCHAR(13)  NOT NULL,
    jurisdiccion_id  BIGINT       NOT NULL,
    contacto         VARCHAR(100),
    email            VARCHAR(100),
    telefono         VARCHAR(20),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_cliente_jurisdiccion FOREIGN KEY (jurisdiccion_id) REFERENCES jurisdiccion(id),
    CONSTRAINT uk_cliente_tenant_cuit UNIQUE (tenant_id, cuit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
