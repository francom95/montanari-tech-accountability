-- F1.3 — estructura base: tenant, usuario, auditoria_log (F1.1 secciones 2, 4 y 7).
-- Roles se modelan como columna enum (usuario.rol), no como tabla aparte (F1.1 diccionario de datos).

CREATE TABLE tenant (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(160)    NOT NULL,
    cuit            VARCHAR(20)     NULL,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       DATETIME(6)     NOT NULL,
    creado_por      VARCHAR(120)    NULL,
    actualizado_en  DATETIME(6)     NOT NULL,
    actualizado_por VARCHAR(120)    NULL,
    version         BIGINT          NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE usuario (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    email           VARCHAR(160)    NOT NULL,
    nombre          VARCHAR(160)    NOT NULL,
    password_hash   VARCHAR(100)    NOT NULL,
    rol             VARCHAR(20)     NOT NULL,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    ultimo_login_en DATETIME(6)     NULL,
    creado_en       DATETIME(6)     NOT NULL,
    creado_por      VARCHAR(120)    NULL,
    actualizado_en  DATETIME(6)     NOT NULL,
    actualizado_por VARCHAR(120)    NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_usuario_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT uk_usuario_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMINISTRADOR', 'CARGA', 'LECTURA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE auditoria_log (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id              BIGINT          NOT NULL,
    entidad_tipo           VARCHAR(60)     NOT NULL,
    entidad_id             BIGINT          NOT NULL,
    accion                 VARCHAR(30)     NOT NULL,
    usuario_id             BIGINT          NULL,
    fecha_hora             DATETIME(6)     NOT NULL,
    datos_antes            JSON            NULL,
    datos_despues          JSON            NULL,
    sobre_periodo_cerrado  BOOLEAN         NOT NULL DEFAULT FALSE,
    detalle                VARCHAR(500)    NULL,
    CONSTRAINT fk_auditoria_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_auditoria_entidad ON auditoria_log (tenant_id, entidad_tipo, entidad_id);
CREATE INDEX ix_auditoria_fecha ON auditoria_log (tenant_id, fecha_hora);
CREATE INDEX ix_auditoria_usuario ON auditoria_log (usuario_id);

-- Seed: tenant único de esta etapa (fila 1 = Montanari Tech, F1.1 §2).
INSERT INTO tenant (nombre, cuit, activo, creado_en, creado_por, actualizado_en, actualizado_por, version)
VALUES ('Montanari Tech', NULL, TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
