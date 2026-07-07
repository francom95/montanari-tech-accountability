-- F3.2 — Plan de cuentas: jerarquía madre/imputable por adjacency list
-- (padre_id), naturaleza fija (5 categorías), rubro (F2.1) obligatorio en
-- imputables, saldo esperado deudor/acreedor, proyectos de uso habitual N:M.

CREATE TABLE cuenta_contable (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT        NOT NULL,
    codigo            VARCHAR(20)   NOT NULL,
    nombre            VARCHAR(160)  NOT NULL,
    padre_id          BIGINT,
    naturaleza        VARCHAR(20)   NOT NULL,
    rubro_id          BIGINT,
    imputable         BOOLEAN       NOT NULL DEFAULT TRUE,
    saldo_esperado    VARCHAR(10)   NOT NULL,
    activo            BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en         DATETIME(6)   NOT NULL,
    creado_por        VARCHAR(120),
    actualizado_en    DATETIME(6)   NOT NULL,
    actualizado_por   VARCHAR(120),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_cuenta_contable_tenant_codigo UNIQUE (tenant_id, codigo),
    CONSTRAINT fk_cuenta_contable_padre FOREIGN KEY (padre_id) REFERENCES cuenta_contable(id),
    CONSTRAINT fk_cuenta_contable_rubro FOREIGN KEY (rubro_id) REFERENCES rubro(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cuenta_contable_proyecto (
    cuenta_contable_id  BIGINT NOT NULL,
    proyecto_id         BIGINT NOT NULL,
    PRIMARY KEY (cuenta_contable_id, proyecto_id),
    CONSTRAINT fk_cuenta_contable_proyecto_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id) ON DELETE CASCADE,
    CONSTRAINT fk_cuenta_contable_proyecto_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
