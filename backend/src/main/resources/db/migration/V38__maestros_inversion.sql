-- F8.4 — Inversiones (Fondo Fima y similares, PL-1). Vínculo opcional
-- polimórfico a Compromiso/Vencimiento (vinculo_tipo/vinculo_ref_id, sin FK,
-- mismo patrón que atribucion_impuesto.liquidacion_tipo/liquidacion_id) para
-- que F8.3 proyecte el rescate planificado como ingreso en esa fecha.

CREATE TABLE inversion (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    instrumento         VARCHAR(120)  NOT NULL,
    cuenta_origen_id    BIGINT        NOT NULL,
    objetivo_del_dinero VARCHAR(200),
    vinculo_tipo        VARCHAR(20),
    vinculo_ref_id      BIGINT,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    activo              BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en           DATETIME(6)   NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6)   NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_inversion_cuenta_origen FOREIGN KEY (cuenta_origen_id) REFERENCES cuenta_bancaria(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_inversion_estado ON inversion (tenant_id, estado);
CREATE INDEX ix_inversion_vinculo ON inversion (tenant_id, vinculo_tipo, vinculo_ref_id);

CREATE TABLE movimiento_inversion (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT        NOT NULL,
    inversion_id          BIGINT        NOT NULL,
    tipo                  VARCHAR(20)   NOT NULL,
    fecha                 DATE          NOT NULL,
    monto_aplicado        DECIMAL(18,2) NOT NULL,
    cuotapartes           DECIMAL(18,6) NOT NULL,
    valor_cuotaparte      DECIMAL(19,6) NOT NULL,
    fecha_liquidacion     DATE,
    movimiento_bancario_id BIGINT,
    observaciones         VARCHAR(500),
    creado_en             DATETIME(6)   NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)   NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_movimiento_inversion_inversion FOREIGN KEY (inversion_id) REFERENCES inversion(id),
    CONSTRAINT fk_movimiento_inversion_mov_bancario FOREIGN KEY (movimiento_bancario_id) REFERENCES movimiento_bancario(id),
    CONSTRAINT uk_movimiento_inversion_mov_bancario UNIQUE (movimiento_bancario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_movimiento_inversion_inversion ON movimiento_inversion (tenant_id, inversion_id, fecha);
