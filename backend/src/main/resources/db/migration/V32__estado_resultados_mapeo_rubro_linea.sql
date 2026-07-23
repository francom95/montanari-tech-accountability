CREATE TABLE mapeo_rubro_linea_er (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    rubro_id         BIGINT       NOT NULL,
    naturaleza       VARCHAR(20)  NOT NULL,
    linea            VARCHAR(40)  NOT NULL,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_mapeo_rubro_linea_er UNIQUE (tenant_id, rubro_id, naturaleza),
    CONSTRAINT fk_mapeo_rubro_linea_er_rubro FOREIGN KEY (rubro_id) REFERENCES rubro(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed con los 4 rubros de resultado ya sembrados en V17 (F7.3): la clave es
-- (rubro, naturaleza) y no solo rubro porque "Otros Ingresos y Egresos" tiene
-- cuentas hijas RP (comisiones ganadas) y RN (comisiones bancarias) mezcladas
-- bajo un mismo rubro — separarlas por naturaleza es lo único que permite
-- distinguir "Otros ingresos" de "Otros egresos" sin tocar el plan de cuentas.
-- "Gastos Operativos" junta sueldos/honorarios/intereses/impuestos en un solo
-- rubro (no separado en comercialización/administración/financieros/impuestos
-- como pide el ER de 9 líneas); se mapea entero a GASTOS_DE_ADMINISTRACION
-- como default aceptado — el admin puede reclasificar cuentas a rubros más
-- finos con el CRUD existente y editar este mapeo, sin que este paso reescriba
-- datos de referencia que F3.3 ya decidió.
INSERT INTO mapeo_rubro_linea_er (tenant_id, rubro_id, naturaleza, linea, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Ingresos por servicios brutos'), 'RP', 'INGRESOS_POR_VENTAS', UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Costos de prestacion por servicios'), 'RN', 'COSTOS_DE_PRESTACION_DE_SERVICIOS', UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Gastos Operativos'), 'RN', 'GASTOS_DE_ADMINISTRACION', UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Otros Ingresos y Egresos'), 'RP', 'OTROS_INGRESOS', UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Otros Ingresos y Egresos'), 'RN', 'OTROS_EGRESOS', UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
