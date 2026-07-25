-- F8.5 — Pendiente administrativo (PL-1): recordatorios, controles manuales,
-- revisiones del contador, ajustes pendientes, facturas a pedir, pagos a
-- verificar, movimientos bancarios a identificar, impuestos a revisar.
-- fecha_estimada_resolucion alimenta la query de alertas (F9.1).

CREATE TABLE pendiente_administrativo (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT        NOT NULL,
    titulo                      VARCHAR(200)  NOT NULL,
    descripcion                 VARCHAR(2000),
    fecha_estimada_resolucion   DATE,
    prioridad                   VARCHAR(10)   NOT NULL DEFAULT 'MEDIA',
    estado                      VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    responsable_id              BIGINT,
    categoria                   VARCHAR(100),
    proyecto_id                 BIGINT,
    cliente_id                  BIGINT,
    proveedor_id                BIGINT,
    observaciones               VARCHAR(2000),
    activo                      BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en                   DATETIME(6)   NOT NULL,
    creado_por                  VARCHAR(120),
    actualizado_en              DATETIME(6)   NOT NULL,
    actualizado_por             VARCHAR(120),
    version                     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_pendiente_responsable FOREIGN KEY (responsable_id) REFERENCES usuario(id),
    CONSTRAINT fk_pendiente_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_pendiente_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_pendiente_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_pendiente_estado ON pendiente_administrativo (tenant_id, estado);
CREATE INDEX ix_pendiente_prioridad ON pendiente_administrativo (tenant_id, prioridad);
CREATE INDEX ix_pendiente_fecha_estimada ON pendiente_administrativo (tenant_id, fecha_estimada_resolucion);
