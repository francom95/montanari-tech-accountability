-- F7.5 — Dashboard: config de vencimientos fijos (IVA/IIBB) y ventana de
-- "obligaciones próximas", una sola fila por tenant, editable por admin.
CREATE TABLE configuracion_dashboard (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT      NOT NULL,
    dia_vencimiento_iva         INT         NOT NULL DEFAULT 20,
    dia_vencimiento_iibb        INT         NOT NULL DEFAULT 15,
    ventana_obligaciones_dias   INT         NOT NULL DEFAULT 15,
    creado_en                   DATETIME(6) NOT NULL,
    creado_por                  VARCHAR(120),
    actualizado_en              DATETIME(6) NOT NULL,
    actualizado_por             VARCHAR(120),
    version                     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_configuracion_dashboard_tenant UNIQUE (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO configuracion_dashboard (tenant_id, dia_vencimiento_iva, dia_vencimiento_iibb, ventana_obligaciones_dias, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 20, 15, 15, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
