-- F2.1 — Rubro: nombre, categoría, orden, activo.

CREATE TABLE rubro (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    nombre           VARCHAR(80)  NOT NULL,
    categoria_id     BIGINT       NOT NULL,
    orden            INT          NOT NULL DEFAULT 0,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_rubro_categoria FOREIGN KEY (categoria_id) REFERENCES categoria_contable(id),
    CONSTRAINT uk_rubro_tenant_nombre UNIQUE (tenant_id, nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
