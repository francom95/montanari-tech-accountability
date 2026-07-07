-- F2.7 — Comisionista: maestro simple (nombre, CUIT opcional, contacto) + vinculo
-- N:M con proyecto que lleva atributos propios (comision_proyecto): porcentaje,
-- base de calculo, moneda, importe estimado/final, estado de pago.

CREATE TABLE comisionista (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    nombre           VARCHAR(120) NOT NULL,
    cuit             VARCHAR(13),
    contacto         VARCHAR(100),
    email            VARCHAR(100),
    telefono         VARCHAR(20),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_comisionista_tenant_cuit UNIQUE (tenant_id, cuit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comision_proyecto (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    proyecto_id           BIGINT        NOT NULL,
    comisionista_id       BIGINT        NOT NULL,
    porcentaje_comision   DECIMAL(5,2)  NOT NULL,
    base_calculo          VARCHAR(30)   NOT NULL,
    moneda_id             BIGINT        NOT NULL,
    importe_estimado      DECIMAL(18,2) NOT NULL,
    importe_final         DECIMAL(18,2),
    estado_pago           VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_estimada_pago   DATE,
    observaciones         VARCHAR(2000),
    activo                BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en             DATETIME(6)   NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)   NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_comision_proyecto_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id) ON DELETE CASCADE,
    CONSTRAINT fk_comision_proyecto_comisionista FOREIGN KEY (comisionista_id) REFERENCES comisionista(id),
    CONSTRAINT fk_comision_proyecto_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
