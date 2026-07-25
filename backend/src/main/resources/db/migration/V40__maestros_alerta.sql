-- F9.1 — Motor de alertas: Alerta (sincronizada por tipo+entidad, sin
-- duplicar), AlertaLectura (lectura por usuario) y ConfiguracionAlertas
-- (parámetros, una fila por tenant, mismo molde que configuracion_dashboard).
CREATE TABLE alerta (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    tipo                VARCHAR(40)  NOT NULL,
    severidad           VARCHAR(20)  NOT NULL,
    mensaje             VARCHAR(300) NOT NULL,
    entidad_tipo        VARCHAR(30)  NOT NULL,
    entidad_ref_id      BIGINT       NOT NULL,
    fecha               DATE         NOT NULL,
    estado              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA',
    resuelta_en         DATETIME(6),
    creado_en           DATETIME(6)  NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6)  NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_alerta_tenant_tipo_entidad UNIQUE (tenant_id, tipo, entidad_tipo, entidad_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_alerta_estado ON alerta (estado);

CREATE TABLE alerta_lectura (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT      NOT NULL,
    alerta_id           BIGINT      NOT NULL,
    usuario_id          BIGINT      NOT NULL,
    leida_en            DATETIME(6) NOT NULL,
    creado_en           DATETIME(6) NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6) NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_alerta_lectura_alerta_usuario UNIQUE (alerta_id, usuario_id),
    CONSTRAINT fk_alerta_lectura_alerta FOREIGN KEY (alerta_id) REFERENCES alerta (id),
    CONSTRAINT fk_alerta_lectura_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE configuracion_alertas (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    dias_anticipacion   INT    NOT NULL DEFAULT 7,
    dias_atraso_cxc     INT    NOT NULL DEFAULT 0,
    creado_en           DATETIME(6) NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6) NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_configuracion_alertas_tenant UNIQUE (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO configuracion_alertas (tenant_id, dias_anticipacion, dias_atraso_cxc, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 7, 0, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
