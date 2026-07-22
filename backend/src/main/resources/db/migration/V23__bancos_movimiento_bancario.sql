-- F5.1 — Movimientos bancarios y bandeja "pendiente de revisar": entra
-- siempre como PENDIENTE, nunca impacta la contabilidad hasta que el
-- usuario confirma/asocia/imputa/descarta explícitamente (F5.1).

CREATE TABLE movimiento_bancario (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT        NOT NULL,
    cuenta_bancaria_id          BIGINT        NOT NULL,
    fecha                       DATE          NOT NULL,
    descripcion                 VARCHAR(500)  NOT NULL,
    importe                     DECIMAL(18,2) NOT NULL,
    moneda_id                   BIGINT        NOT NULL,
    tipo_cambio                 DECIMAL(19,6) NOT NULL,
    fuente_tc                   VARCHAR(20),
    importe_ars                 DECIMAL(18,2) NOT NULL,
    referencia                  VARCHAR(100),
    origen_importacion          VARCHAR(20)   NOT NULL,
    cuenta_contable_sugerida_id BIGINT,
    estado                      VARCHAR(20)   NOT NULL,
    asiento_id                  BIGINT,
    motivo_descarte             VARCHAR(500),
    observaciones               VARCHAR(2000),
    creado_en                   DATETIME(6)   NOT NULL,
    creado_por                  VARCHAR(120),
    actualizado_en              DATETIME(6)   NOT NULL,
    actualizado_por             VARCHAR(120),
    version                     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_movimiento_bancario_asiento UNIQUE (asiento_id),
    CONSTRAINT fk_movimiento_bancario_cuenta_bancaria FOREIGN KEY (cuenta_bancaria_id) REFERENCES cuenta_bancaria(id),
    CONSTRAINT fk_movimiento_bancario_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_movimiento_bancario_cuenta_sugerida FOREIGN KEY (cuenta_contable_sugerida_id) REFERENCES cuenta_contable(id),
    CONSTRAINT fk_movimiento_bancario_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_movimiento_bancario_cuenta_estado ON movimiento_bancario (tenant_id, cuenta_bancaria_id, estado);
