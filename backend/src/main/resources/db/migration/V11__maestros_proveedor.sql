-- F2.3 — Proveedor: nombre, CUIT, jurisdicción, moneda habitual (opcional), tipos de costo (N:M), contacto, activo.

CREATE TABLE proveedor (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    nombre              VARCHAR(120) NOT NULL,
    cuit                VARCHAR(13)  NOT NULL,
    jurisdiccion_id     BIGINT       NOT NULL,
    moneda_habitual_id  BIGINT,
    contacto            VARCHAR(100),
    email               VARCHAR(100),
    telefono            VARCHAR(20),
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en           DATETIME(6)  NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6)  NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_proveedor_jurisdiccion FOREIGN KEY (jurisdiccion_id) REFERENCES jurisdiccion(id),
    CONSTRAINT fk_proveedor_moneda FOREIGN KEY (moneda_habitual_id) REFERENCES moneda(id),
    CONSTRAINT uk_proveedor_tenant_cuit UNIQUE (tenant_id, cuit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE proveedor_tipo_costo (
    proveedor_id  BIGINT NOT NULL,
    tipo_costo_id BIGINT NOT NULL,
    PRIMARY KEY (proveedor_id, tipo_costo_id),
    CONSTRAINT fk_proveedor_tipo_costo_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id) ON DELETE CASCADE,
    CONSTRAINT fk_proveedor_tipo_costo_tipo_costo FOREIGN KEY (tipo_costo_id) REFERENCES tipo_costo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
