-- F7.4 — Infraestructura transversal para el reporte de rentabilidad por
-- proyecto: (1) criterio de TC por defecto configurable (hoy
-- resolverTipoCambioAutomatico ignora el criterio, gap documentado en
-- TipoCambioRepository); (2) mora en cobros — nueva línea contable real de
-- "Interés ganado" cuando un cobro llega con atraso.

-- 1) Config de TC por defecto (una fila por tenant, criterio_por_defecto NULL
-- preserva el comportamiento actual).
CREATE TABLE configuracion_tipo_cambio (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT       NOT NULL,
    criterio_por_defecto  VARCHAR(50),
    creado_en             DATETIME(6)  NOT NULL,
    creado_por            VARCHAR(120),
    actualizado_en        DATETIME(6)  NOT NULL,
    actualizado_por       VARCHAR(120),
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_configuracion_tipo_cambio_tenant UNIQUE (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO configuracion_tipo_cambio (tenant_id, criterio_por_defecto, creado_en, creado_por, actualizado_en, actualizado_por, version)
VALUES (1, NULL, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- 2) Config de cobranza (mora): tasa 0 por defecto = sin recargo hasta que
-- el admin cargue una tasa real.
CREATE TABLE configuracion_cobranza (
    id                            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                     BIGINT        NOT NULL,
    dias_gracia_mora              INT           NOT NULL DEFAULT 3,
    tasa_mora_diaria_porcentaje   DECIMAL(9,7)  NOT NULL DEFAULT 0,
    creado_en                     DATETIME(6)   NOT NULL,
    creado_por                    VARCHAR(120),
    actualizado_en                DATETIME(6)   NOT NULL,
    actualizado_por               VARCHAR(120),
    version                       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_configuracion_cobranza_tenant UNIQUE (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO configuracion_cobranza (tenant_id, dias_gracia_mora, tasa_mora_diaria_porcentaje, creado_en, creado_por, actualizado_en, actualizado_por, version)
VALUES (1, 3, 0, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- 3) Recargo por mora por imputación (NULL/0 en todo cobro existente ⇒ cero
-- cambio de comportamiento en CobroAsientoGenerator).
ALTER TABLE cobro_imputacion ADD COLUMN recargo_mora_original DECIMAL(18,2) NULL AFTER monto_ars_cancelado;

-- 4) Mapeo del nuevo concepto INTERES_POR_MORA_GANADO a la cuenta ya
-- sembrada en V17 ("6.4002 Intereses Ganados"), nunca usada hasta ahora.
INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'INTERES_POR_MORA_GANADO', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '6.4002'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
