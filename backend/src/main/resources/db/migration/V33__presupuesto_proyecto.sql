-- Presupuesto estimado por proyecto (F2.6): repite el waterfall de la
-- planilla real del contador (GUADA / GUADA MODELO), no la de FRAN, que tiene
-- una constante hardcodeada (363/24637) en vez de aplicar el 1,2% de forma
-- paramétrica. Las alícuotas y el esquema de comisión bancaria COMEX quedan
-- en configuracion_presupuesto, una sola fila por tenant, editable por admin.
CREATE TABLE configuracion_presupuesto (
    id                                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                                   BIGINT       NOT NULL,
    comision_venta_porcentaje                   DECIMAL(7,5) NOT NULL,
    colchon_impuesto_ganancias_porcentaje        DECIMAL(7,5) NOT NULL,
    iibb_convenio_multilateral_porcentaje       DECIMAL(7,5) NOT NULL,
    impuesto_debitos_creditos_porcentaje        DECIMAL(7,5) NOT NULL,
    iva_porcentaje                              DECIMAL(7,5) NOT NULL,
    diferencia_dolar_comercializacion_porcentaje DECIMAL(7,5) NOT NULL,
    percepcion_iva_comex_porcentaje             DECIMAL(7,5) NOT NULL,
    iibb_sircreb_comex_porcentaje                DECIMAL(7,5) NOT NULL,
    comex_umbral_uno_usd                        DECIMAL(18,2) NOT NULL,
    comex_monto_uno_usd                         DECIMAL(18,2) NOT NULL,
    comex_umbral_dos_usd                        DECIMAL(18,2) NOT NULL,
    comex_monto_dos_usd                         DECIMAL(18,2) NOT NULL,
    comex_umbral_tres_usd                       DECIMAL(18,2) NOT NULL,
    comex_monto_tres_usd                        DECIMAL(18,2) NOT NULL,
    comex_porcentaje_excedente                  DECIMAL(7,5) NOT NULL,
    creado_en                                   DATETIME(6)  NOT NULL,
    creado_por                                  VARCHAR(120),
    actualizado_en                              DATETIME(6)  NOT NULL,
    actualizado_por                             VARCHAR(120),
    version                                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_configuracion_presupuesto_tenant UNIQUE (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Valores tal como figuran en la planilla real (GUADA / GUADA MODELO).
INSERT INTO configuracion_presupuesto (
    tenant_id, comision_venta_porcentaje, colchon_impuesto_ganancias_porcentaje,
    iibb_convenio_multilateral_porcentaje, impuesto_debitos_creditos_porcentaje, iva_porcentaje,
    diferencia_dolar_comercializacion_porcentaje, percepcion_iva_comex_porcentaje, iibb_sircreb_comex_porcentaje,
    comex_umbral_uno_usd, comex_monto_uno_usd, comex_umbral_dos_usd, comex_monto_dos_usd,
    comex_umbral_tres_usd, comex_monto_tres_usd, comex_porcentaje_excedente,
    creado_en, creado_por, actualizado_en, actualizado_por, version
) VALUES (
    1, 0.10000, 0.30000, 0.05000, 0.01200, 0.21000,
    0.05000, 0.03000, 0.04000,
    100.00, 10.00, 500.00, 30.00, 1000.00, 50.00, 0.00125,
    UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0
);

-- Presupuesto por proyecto (1:1). Los cálculos derivados (comisión, IG,
-- impuestos, precio final) NO se persisten: se recalculan on-demand a partir
-- de estos inputs + configuracion_presupuesto vigente + Proyecto.tipoProyecto
-- (editable en vivo, sin ciclo de estados, tal como pide el plan).
CREATE TABLE presupuesto_proyecto (
    id                                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                                BIGINT        NOT NULL,
    proyecto_id                              BIGINT        NOT NULL,
    margen_deseado_usd                       DECIMAL(18,2) NOT NULL,
    comisiones_bancarias_intermedias_comex_usd DECIMAL(18,2),
    observaciones                            VARCHAR(2000),
    creado_en                                DATETIME(6)   NOT NULL,
    creado_por                               VARCHAR(120),
    actualizado_en                           DATETIME(6)   NOT NULL,
    actualizado_por                          VARCHAR(120),
    version                                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_presupuesto_proyecto_proyecto UNIQUE (proyecto_id),
    CONSTRAINT fk_presupuesto_proyecto_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Costos de producción: líneas libres (nombre + importe USD), no roles fijos
-- -- el Excel real varía los roles proyecto a proyecto (Desarrolladores,
-- Pentesting, Tester QA en un caso; solo "Desarrolladores" en otro).
CREATE TABLE presupuesto_linea_costo (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                BIGINT        NOT NULL,
    presupuesto_proyecto_id  BIGINT        NOT NULL,
    nombre                   VARCHAR(160)  NOT NULL,
    importe_usd              DECIMAL(18,2) NOT NULL,
    orden                    INT           NOT NULL DEFAULT 0,
    creado_en                DATETIME(6)   NOT NULL,
    creado_por               VARCHAR(120),
    actualizado_en           DATETIME(6)   NOT NULL,
    actualizado_por          VARCHAR(120),
    version                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_presupuesto_linea_costo_presupuesto FOREIGN KEY (presupuesto_proyecto_id) REFERENCES presupuesto_proyecto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
