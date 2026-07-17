-- F4.2 — Infraestructura compartida de facturación (F4.1): las 4 cuentas
-- que el motor requiere y el seed F3.3 no traía (confirmadas en checkpoint),
-- el mapeo concepto->cuenta, adjuntos genéricos y tributos de comprobante.
-- Reusada tal cual por F4.3 (facturas de compra) y F4.4 (cobros y pagos).

-- 1) Las 4 cuentas confirmadas por el contador en el checkpoint de F4.1 §3.
INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '6.4005', 'Diferencia de cambio ganada', NULL, 'RP', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '6.4006', 'Diferencia de cambio perdida', NULL, 'RN', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '2.1.2018', 'Anticipos de clientes', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, '1.1.2013', 'Anticipos a proveedores', NULL, 'ACTIVO', NULL, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = 'Otros Ingresos y Egresos')
    WHERE tenant_id = 1 AND codigo IN ('6.4005', '6.4006');
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '5. Otras Deudas')
    WHERE tenant_id = 1 AND codigo = '2.1.2018';
UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '3. Otros Créditos por ventas')
    WHERE tenant_id = 1 AND codigo = '1.1.2013';

UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '6'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo IN ('6.4005', '6.4006');
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '2.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo = '2.1.2018';
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '1.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo = '1.1.2013';

-- 2) Mapeo concepto->cuenta (F4.1 §1): el motor nunca hardcodea el plan de
-- cuentas. discriminador_tipo NULL = fila por defecto del concepto.
CREATE TABLE mapeo_cuenta (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    concepto             VARCHAR(40)  NOT NULL,
    discriminador_tipo   VARCHAR(30),
    discriminador_valor  VARCHAR(60),
    cuenta_contable_id   BIGINT       NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en            DATETIME(6)  NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)  NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_mapeo_cuenta UNIQUE (tenant_id, concepto, discriminador_tipo, discriminador_valor),
    CONSTRAINT fk_mapeo_cuenta_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed mínimo: los conceptos que ya usa el generador de factura de venta (F4.2).
-- CREDITO_POR_VENTA no tiene fila por defecto a propósito (F4.1 §2.1): el
-- checkpoint confirmó mantener cuentas por cliente (Cliente.cuenta_cxc_id);
-- inventar una cuenta "genérica" no fue parte de lo decidido.
INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'IVA_DEBITO_FISCAL', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '2.1.2008'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'INGRESO_VENTA', 'TIPO_INGRESO', 'VENTA', (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '4.1.2001'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'INGRESO_VENTA', 'TIPO_INGRESO', 'OTRA_VENTA', (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '4.1.2002'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- 3) Adjuntos genéricos (F1.1 §6.5): archivos en filesystem/volumen Docker,
-- nunca BLOB en MySQL. Opcional en facturas (funcional §5.4).
CREATE TABLE adjunto (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    entidad_tipo     VARCHAR(40)  NOT NULL,
    entidad_id       BIGINT       NOT NULL,
    nombre_archivo   VARCHAR(255) NOT NULL,
    ruta             VARCHAR(500) NOT NULL,
    mime             VARCHAR(100) NOT NULL,
    tamanio          BIGINT       NOT NULL,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_adjunto_entidad ON adjunto (tenant_id, entidad_tipo, entidad_id);

-- 4) Tributos de comprobante (F1.1 §6.5): percepciones/retenciones/SIRCREB de
-- cualquier comprobante. Captura informativa para F6.1/F6.2; no todo tipo de
-- comprobante genera impacto contable propio (F4.1 confirmó que la factura
-- de venta no lleva percepciones/retenciones practicadas).
CREATE TABLE comprobante_tributo (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT        NOT NULL,
    comprobante_tipo  VARCHAR(20)   NOT NULL,
    comprobante_id    BIGINT        NOT NULL,
    tipo              VARCHAR(30)   NOT NULL,
    jurisdiccion_id   BIGINT,
    base              DECIMAL(18,2),
    alicuota          DECIMAL(5,2),
    importe           DECIMAL(18,2) NOT NULL,
    creado_en         DATETIME(6)   NOT NULL,
    creado_por        VARCHAR(120),
    actualizado_en    DATETIME(6)   NOT NULL,
    actualizado_por   VARCHAR(120),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_comprobante_tributo_jurisdiccion FOREIGN KEY (jurisdiccion_id) REFERENCES jurisdiccion(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_comprobante_tributo_comprobante ON comprobante_tributo (tenant_id, comprobante_tipo, comprobante_id);

-- 5) Cuenta de créditos por ventas por cliente (F4.1 §2.1, checkpoint
-- confirmado: opción A, mantener cuentas por cliente). Opcional: clientes
-- sin cuenta propia asignada dependen de la fila por defecto de
-- CREDITO_POR_VENTA en mapeo_cuenta (hoy ausente a propósito).
ALTER TABLE cliente ADD COLUMN cuenta_cxc_id BIGINT NULL AFTER telefono;
ALTER TABLE cliente ADD CONSTRAINT fk_cliente_cuenta_cxc FOREIGN KEY (cuenta_cxc_id) REFERENCES cuenta_contable(id);

-- 6) Factura de venta: cabecera + líneas (F1.1 §6.5).
CREATE TABLE factura_venta (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id              BIGINT        NOT NULL,
    cliente_id             BIGINT        NOT NULL,
    proyecto_id            BIGINT,
    fecha                  DATE          NOT NULL,
    fecha_vencimiento      DATE,
    tipo_comprobante       VARCHAR(20)   NOT NULL,
    punto_venta            VARCHAR(20),
    numero                 VARCHAR(20)   NOT NULL,
    jurisdiccion_destino_id BIGINT,
    moneda_id              BIGINT        NOT NULL,
    tipo_cambio            DECIMAL(19,6) NOT NULL,
    fuente_tc              VARCHAR(20),
    neto_gravado           DECIMAL(18,2) NOT NULL DEFAULT 0,
    no_gravado             DECIMAL(18,2) NOT NULL DEFAULT 0,
    exento                 DECIMAL(18,2) NOT NULL DEFAULT 0,
    importe_iva            DECIMAL(18,2) NOT NULL DEFAULT 0,
    total                  DECIMAL(18,2) NOT NULL,
    total_ars              DECIMAL(18,2) NOT NULL,
    estado                 VARCHAR(20)   NOT NULL,
    asiento_id             BIGINT,
    observaciones          VARCHAR(2000),
    creado_en              DATETIME(6)   NOT NULL,
    creado_por             VARCHAR(120),
    actualizado_en         DATETIME(6)   NOT NULL,
    actualizado_por        VARCHAR(120),
    version                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_factura_venta UNIQUE (tenant_id, cliente_id, tipo_comprobante, punto_venta, numero),
    CONSTRAINT fk_factura_venta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_factura_venta_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_factura_venta_jurisdiccion FOREIGN KEY (jurisdiccion_destino_id) REFERENCES jurisdiccion(id),
    CONSTRAINT fk_factura_venta_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_factura_venta_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_factura_venta_tenant_fecha ON factura_venta (tenant_id, fecha);
CREATE INDEX ix_factura_venta_cliente ON factura_venta (cliente_id);

CREATE TABLE factura_venta_linea (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    factura_venta_id    BIGINT        NOT NULL,
    orden               INT           NOT NULL,
    descripcion         VARCHAR(500)  NOT NULL,
    tipo                VARCHAR(20)   NOT NULL,
    tipo_ingreso        VARCHAR(20)   NOT NULL DEFAULT 'VENTA',
    importe_neto        DECIMAL(18,2) NOT NULL,
    alicuota_iva        DECIMAL(5,2)  NOT NULL,
    importe_iva         DECIMAL(18,2) NOT NULL,
    cuenta_contable_id  BIGINT,
    creado_en           DATETIME(6)   NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6)   NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_factura_venta_linea_factura FOREIGN KEY (factura_venta_id) REFERENCES factura_venta(id) ON DELETE CASCADE,
    CONSTRAINT fk_factura_venta_linea_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_factura_venta_linea_factura ON factura_venta_linea (factura_venta_id);
