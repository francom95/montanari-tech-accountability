-- F4.3 — Facturas de compra: condición de IVA y cuenta CxP por proveedor
-- (F4.1 §2.2, §5), mapeo_cuenta de los conceptos globales que ya tenían
-- cuenta fija en el seed F3.3 (crédito fiscal, percepciones, retenciones
-- sufridas), y cabecera + líneas de factura_compra (F1.1 §6.5).

-- 1) Condición de IVA y cuenta de deudas comerciales propia del proveedor.
ALTER TABLE proveedor ADD COLUMN condicion_iva VARCHAR(30) NOT NULL DEFAULT 'RESPONSABLE_INSCRIPTO' AFTER telefono;
ALTER TABLE proveedor ADD COLUMN cuenta_cxp_id BIGINT NULL AFTER condicion_iva;
ALTER TABLE proveedor ADD CONSTRAINT fk_proveedor_cuenta_cxp FOREIGN KEY (cuenta_cxp_id) REFERENCES cuenta_contable(id);

-- 2) Mapeo concepto->cuenta (F4.1 §1): estos 4 conceptos apuntan a una única
-- cuenta global del seed F3.3 (no dependen de datos de runtime como
-- COSTO_GASTO/DEUDA_COMERCIAL, por eso sí llevan fila por defecto).
-- RETENCION_GANANCIAS_SUFRIDA no lo usa el generador de compra (solo COBRO,
-- F4.4) pero se siembra ahora junto con el resto de cuentas fijas de F4.1 §1.3.
INSERT INTO mapeo_cuenta (tenant_id, concepto, discriminador_tipo, discriminador_valor, cuenta_contable_id, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, 'IVA_CREDITO_FISCAL', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2006'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'PERCEPCION_IVA_SUFRIDA', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2007'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'PERCEPCION_IIBB_SUFRIDA', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2008'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'RETENCION_GANANCIAS_SUFRIDA', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2011'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0),
    (1, 'RETENCION_IVA_SUFRIDA', NULL, NULL, (SELECT id FROM cuenta_contable WHERE tenant_id = 1 AND codigo = '1.1.2007'), TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

-- Nota: COSTO_GASTO (discriminado por TIPO_COSTO, dato de usuario sin seed
-- propio) y DEUDA_COMERCIAL (sin fila por defecto a propósito, mismo
-- criterio que CREDITO_POR_VENTA en F4.1 §2.1/§2.2) se configuran por el
-- admin vía /mapeos-cuenta una vez creados los tipo_costo y/o las cuentas
-- por proveedor.

-- 3) Factura de compra: cabecera + líneas (F1.1 §6.5, simétrico a F4.2).
CREATE TABLE factura_compra (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id              BIGINT        NOT NULL,
    proveedor_id           BIGINT        NOT NULL,
    proyecto_id            BIGINT,
    fecha                  DATE          NOT NULL,
    fecha_vencimiento      DATE,
    tipo_comprobante       VARCHAR(20)   NOT NULL,
    punto_venta            VARCHAR(20),
    numero                 VARCHAR(20)   NOT NULL,
    moneda_id              BIGINT        NOT NULL,
    tipo_cambio            DECIMAL(19,6) NOT NULL,
    fuente_tc              VARCHAR(20),
    neto                   DECIMAL(18,2) NOT NULL DEFAULT 0,
    importe_iva            DECIMAL(18,2) NOT NULL DEFAULT 0,
    importe_percepciones   DECIMAL(18,2) NOT NULL DEFAULT 0,
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
    CONSTRAINT uk_factura_compra UNIQUE (tenant_id, proveedor_id, tipo_comprobante, punto_venta, numero),
    CONSTRAINT fk_factura_compra_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_factura_compra_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_factura_compra_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_factura_compra_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_factura_compra_tenant_fecha ON factura_compra (tenant_id, fecha);
CREATE INDEX ix_factura_compra_proveedor ON factura_compra (proveedor_id);

CREATE TABLE factura_compra_linea (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    factura_compra_id   BIGINT        NOT NULL,
    orden               INT           NOT NULL,
    descripcion         VARCHAR(500)  NOT NULL,
    tipo_costo_id       BIGINT        NOT NULL,
    importe_neto        DECIMAL(18,2) NOT NULL,
    alicuota_iva        DECIMAL(5,2)  NOT NULL,
    importe_iva         DECIMAL(18,2) NOT NULL,
    cuenta_contable_id  BIGINT,
    creado_en           DATETIME(6)   NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6)   NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_factura_compra_linea_factura FOREIGN KEY (factura_compra_id) REFERENCES factura_compra(id) ON DELETE CASCADE,
    CONSTRAINT fk_factura_compra_linea_tipo_costo FOREIGN KEY (tipo_costo_id) REFERENCES tipo_costo(id),
    CONSTRAINT fk_factura_compra_linea_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_factura_compra_linea_factura ON factura_compra_linea (factura_compra_id);
