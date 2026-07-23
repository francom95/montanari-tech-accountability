-- F6.3 — Atribución de impuestos liquidados (IVA, IIBB) a proyectos. La
-- referencia a la liquidación es polimórfica (liquidacion_tipo + liquidacion_id
-- sin FK real), mismo patrón que comprobante_tributo (F4.1): así una atribución
-- sirve para IVA y para IIBB sin tocar esas tablas. Se persiste para que el
-- reporte de rentabilidad por proyecto (F7.4) la consuma sin recalcular.

-- Configuración del criterio de prorrateo por defecto del sistema (editable por
-- admin). Una sola fila por tenant; se puede overridear por liquidación.
CREATE TABLE configuracion_atribucion (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id              BIGINT       NOT NULL,
    criterio_por_defecto   VARCHAR(30)  NOT NULL,
    creado_en              DATETIME(6)  NOT NULL,
    creado_por             VARCHAR(120),
    actualizado_en         DATETIME(6)  NOT NULL,
    actualizado_por        VARCHAR(120),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_configuracion_atribucion_tenant UNIQUE (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO configuracion_atribucion (tenant_id, criterio_por_defecto, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'FACTURACION', UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- Cabecera de la atribución de una liquidación. monto_total es el impuesto a
-- repartir (saldo a pagar de la liquidación), siempre en ARS — los impuestos se
-- liquidan en pesos, así que moneda=ARS y tc=1, pero se guardan igual por la
-- regla multimoneda. anio/mes se copian para que F7.4 consulte por período.
CREATE TABLE atribucion_impuesto (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT        NOT NULL,
    liquidacion_tipo  VARCHAR(10)   NOT NULL,
    liquidacion_id    BIGINT        NOT NULL,
    anio              INT           NOT NULL,
    mes               INT           NOT NULL,
    criterio          VARCHAR(30)   NOT NULL,
    moneda_id         BIGINT        NOT NULL,
    tipo_cambio       DECIMAL(19,6) NOT NULL DEFAULT 1,
    monto_total       DECIMAL(18,2) NOT NULL DEFAULT 0,
    creado_en         DATETIME(6)   NOT NULL,
    creado_por        VARCHAR(120),
    actualizado_en    DATETIME(6)   NOT NULL,
    actualizado_por   VARCHAR(120),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_atribucion_impuesto_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT uk_atribucion_impuesto_liquidacion UNIQUE (tenant_id, liquidacion_tipo, liquidacion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_atribucion_impuesto_periodo ON atribucion_impuesto (tenant_id, anio, mes);

-- Un renglón por proyecto. porcentaje es informativo (puede no sumar 100 exacto
-- por redondeo); lo que suma exacto al monto_total es la columna monto, porque
-- la última línea absorbe el residuo (F6.3, misma regla que CalculoImputacion).
CREATE TABLE atribucion_impuesto_linea (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                BIGINT        NOT NULL,
    atribucion_impuesto_id   BIGINT        NOT NULL,
    proyecto_id              BIGINT        NOT NULL,
    porcentaje               DECIMAL(9,6)  NOT NULL DEFAULT 0,
    monto                    DECIMAL(18,2) NOT NULL DEFAULT 0,
    orden                    INT           NOT NULL,
    creado_en                DATETIME(6)   NOT NULL,
    creado_por               VARCHAR(120),
    actualizado_en           DATETIME(6)   NOT NULL,
    actualizado_por          VARCHAR(120),
    version                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_atribucion_linea_atribucion FOREIGN KEY (atribucion_impuesto_id) REFERENCES atribucion_impuesto(id),
    CONSTRAINT fk_atribucion_linea_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_atribucion_linea_atribucion ON atribucion_impuesto_linea (atribucion_impuesto_id, orden);
CREATE INDEX ix_atribucion_linea_proyecto ON atribucion_impuesto_linea (tenant_id, proyecto_id);
