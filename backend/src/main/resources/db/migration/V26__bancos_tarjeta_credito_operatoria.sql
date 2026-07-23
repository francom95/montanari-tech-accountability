-- F5.4 — Tarjetas de crédito: operatoria completa (importar consumos,
-- clasificarlos, pagar el resumen, conciliar el pago). TarjetaCredito (F2.4)
-- no tenía cuenta contable espejo (a diferencia de CuentaBancaria desde
-- F4.4) — la agrega nullable a propósito: no sabemos si ya existen tarjetas
-- reales creadas por el usuario, así que no se fuerza un backfill adivinado;
-- el alta/edición de tarjeta la exige desde ahora en adelante, y
-- PagoTarjetaService rechaza con un error de negocio claro si falta.

ALTER TABLE tarjeta_credito ADD COLUMN cuenta_contable_id BIGINT NULL AFTER cuenta_bancaria_debito_id;
ALTER TABLE tarjeta_credito ADD CONSTRAINT fk_tarjeta_credito_cuenta_contable FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id);

INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version) VALUES
    (1, '2.1.2019', 'Tarjeta de Crédito a Pagar', NULL, 'PASIVO', NULL, TRUE, 'ACREEDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

UPDATE cuenta_contable SET rubro_id = (SELECT id FROM rubro WHERE tenant_id = 1 AND nombre = '4. Deudas Bancarias')
    WHERE tenant_id = 1 AND codigo = '2.1.2019';
UPDATE cuenta_contable c JOIN cuenta_contable p ON p.tenant_id = 1 AND p.codigo = '2.1'
    SET c.padre_id = p.id WHERE c.tenant_id = 1 AND c.codigo = '2.1.2019';

-- Consumos de tarjeta (F5.4): importados desde el resumen (F5.2, ParserTarjeta
-- reusado), entran sin clasificar (cuenta_contable_id NULL) hasta que el
-- usuario los clasifica a mano o por regla — nunca impactan la contabilidad
-- por sí mismos, solo alimentan el saldo de la tarjeta (importe con signo,
-- igual convención que movimiento_bancario: positivo = consumo/cargo,
-- negativo = devolución/crédito).
CREATE TABLE consumo_tarjeta (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    tarjeta_credito_id   BIGINT        NOT NULL,
    fecha                DATE          NOT NULL,
    descripcion          VARCHAR(500)  NOT NULL,
    referencia           VARCHAR(100),
    importe              DECIMAL(18,2) NOT NULL,
    moneda_id            BIGINT        NOT NULL,
    tipo_cambio          DECIMAL(19,6) NOT NULL,
    importe_ars          DECIMAL(18,2) NOT NULL,
    cuenta_contable_id   BIGINT,
    proveedor_id         BIGINT,
    proyecto_id          BIGINT,
    concepto_id          BIGINT,
    hash_importacion     VARCHAR(64),
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_consumo_tarjeta_tarjeta FOREIGN KEY (tarjeta_credito_id) REFERENCES tarjeta_credito(id),
    CONSTRAINT fk_consumo_tarjeta_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_consumo_tarjeta_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id),
    CONSTRAINT fk_consumo_tarjeta_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_consumo_tarjeta_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_consumo_tarjeta_concepto FOREIGN KEY (concepto_id) REFERENCES concepto_recurrente(id),
    CONSTRAINT uk_consumo_tarjeta_hash UNIQUE (tarjeta_credito_id, hash_importacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_consumo_tarjeta_tarjeta_clasificacion ON consumo_tarjeta (tenant_id, tarjeta_credito_id, cuenta_contable_id);

-- Reglas simples de clasificación masiva (F5.4 §2): "si la descripción
-- contiene X, clasificar con estos datos" — configurable por el usuario, a
-- diferencia de ClasificadorMovimientoBancario (F5.3), que es un catálogo
-- fijo de patrones de resúmenes bancarios argentinos.
CREATE TABLE regla_clasificacion_consumo (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    patron               VARCHAR(200)  NOT NULL,
    cuenta_contable_id   BIGINT        NOT NULL,
    proveedor_id         BIGINT,
    proyecto_id          BIGINT,
    concepto_id          BIGINT,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_regla_clasificacion_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id),
    CONSTRAINT fk_regla_clasificacion_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_regla_clasificacion_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_regla_clasificacion_concepto FOREIGN KEY (concepto_id) REFERENCES concepto_recurrente(id),
    CONSTRAINT uk_regla_clasificacion_tenant_patron UNIQUE (tenant_id, patron)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Pago del resumen de tarjeta (F5.4 §3, molde PL-4/PL-5): cabecera con
-- estado borrador/confirmado/anulado, igual criterio que Cobro/Pago (F4.4).
CREATE TABLE pago_tarjeta (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT        NOT NULL,
    tarjeta_credito_id   BIGINT        NOT NULL,
    fecha                DATE          NOT NULL,
    importe              DECIMAL(18,2) NOT NULL,
    moneda_id            BIGINT        NOT NULL,
    tipo_cambio          DECIMAL(19,6) NOT NULL,
    importe_ars          DECIMAL(18,2) NOT NULL,
    estado               VARCHAR(20)   NOT NULL,
    asiento_id           BIGINT,
    observaciones        VARCHAR(2000),
    creado_en            DATETIME(6)   NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)   NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_pago_tarjeta_tarjeta FOREIGN KEY (tarjeta_credito_id) REFERENCES tarjeta_credito(id),
    CONSTRAINT fk_pago_tarjeta_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_pago_tarjeta_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id),
    CONSTRAINT uk_pago_tarjeta_asiento UNIQUE (asiento_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_pago_tarjeta_tarjeta_estado ON pago_tarjeta (tenant_id, tarjeta_credito_id, estado);
