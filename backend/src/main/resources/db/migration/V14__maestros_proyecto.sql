-- F2.5 — Proyectos y etapas: proyecto (cliente, responsable, moneda, montos,
-- estados comercial/facturación/cobranza), cuotas pactadas (1:N), etapas (1:N,
-- con proveedores N:M).

CREATE TABLE proyecto (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                       BIGINT        NOT NULL,
    nombre                          VARCHAR(160)  NOT NULL,
    cliente_id                      BIGINT        NOT NULL,
    responsable_id                  BIGINT,
    pais                            VARCHAR(80),
    tipo_proyecto                   VARCHAR(80),
    estado                          VARCHAR(20)   NOT NULL DEFAULT 'PROSPECTO',
    moneda_id                       BIGINT        NOT NULL,
    monto_total                     DECIMAL(18,2) NOT NULL,
    cantidad_pagos_pactados         INT,
    comentarios                     VARCHAR(2000),
    estado_comercial                VARCHAR(20)   NOT NULL DEFAULT 'PROSPECTO',
    estado_facturacion              VARCHAR(20)   NOT NULL DEFAULT 'NO_FACTURADO',
    estado_cobranza                 VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_estimada_finalizacion     DATE,
    fecha_real_finalizacion         DATE,
    activo                          BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en                       DATETIME(6)   NOT NULL,
    creado_por                      VARCHAR(120),
    actualizado_en                  DATETIME(6)   NOT NULL,
    actualizado_por                 VARCHAR(120),
    version                         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_proyecto_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_proyecto_responsable FOREIGN KEY (responsable_id) REFERENCES usuario(id),
    CONSTRAINT fk_proyecto_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE proyecto_cuota (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    proyecto_id           BIGINT        NOT NULL,
    numero                INT           NOT NULL,
    fecha_estimada_cobro  DATE          NOT NULL,
    importe               DECIMAL(18,2) NOT NULL,
    creado_en             DATETIME(6)   NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)   NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_proyecto_cuota_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE etapa (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    proyecto_id          BIGINT        NOT NULL,
    nombre               VARCHAR(160)  NOT NULL,
    descripcion          VARCHAR(2000),
    estado               VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_inicio         DATE,
    fecha_estimada_fin   DATE,
    porcentaje_avance    INT,
    monto_presupuestado  DECIMAL(18,2),
    costos_estimados     DECIMAL(18,2),
    pagos_previstos      DECIMAL(18,2),
    cobros_previstos     DECIMAL(18,2),
    observaciones        VARCHAR(2000),
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_etapa_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE etapa_proveedor (
    etapa_id      BIGINT NOT NULL,
    proveedor_id  BIGINT NOT NULL,
    PRIMARY KEY (etapa_id, proveedor_id),
    CONSTRAINT fk_etapa_proveedor_etapa FOREIGN KEY (etapa_id) REFERENCES etapa(id) ON DELETE CASCADE,
    CONSTRAINT fk_etapa_proveedor_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
