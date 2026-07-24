-- F8.2 — Compromiso de pago futuro (PL-1). Alimenta el flujo proyectado de
-- F8.3 como fuente paralela a Vencimiento (F8.1); puede opcionalmente generar
-- su propio vencimiento al crearse (vencimiento_generado_id).

CREATE TABLE compromiso (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id              BIGINT        NOT NULL,
    concepto               VARCHAR(200)  NOT NULL,
    tipo                   VARCHAR(30)   NOT NULL,
    fecha_prevista         DATE          NOT NULL,
    importe                DECIMAL(18,2) NOT NULL,
    moneda_id              BIGINT        NOT NULL,
    proveedor_id           BIGINT,
    proyecto_id            BIGINT,
    estado                 VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    observaciones          VARCHAR(500),
    vencimiento_generado_id BIGINT,
    activo                 BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en              DATETIME(6)   NOT NULL,
    creado_por             VARCHAR(120),
    actualizado_en         DATETIME(6)   NOT NULL,
    actualizado_por        VARCHAR(120),
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_compromiso_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_compromiso_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_compromiso_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_compromiso_vencimiento FOREIGN KEY (vencimiento_generado_id) REFERENCES vencimiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_compromiso_fecha ON compromiso (tenant_id, fecha_prevista);
CREATE INDEX ix_compromiso_estado ON compromiso (tenant_id, estado);
