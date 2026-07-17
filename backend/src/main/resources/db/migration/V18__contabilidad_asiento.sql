-- F3.4 — Motor de asientos manuales (sobre el diseño de F3.1 §3).
-- secuencia: numeración interna correlativa compartida por todas las líneas
-- de un asiento, leída con lock pesimista al confirmar (F3.1 §3.2).
-- asiento: cabecera. numero es NULL en borrador y único cuando se asigna
-- (MySQL no exige unicidad entre NULLs, así que múltiples borradores conviven).
-- asiento_linea: dimensiones analíticas opcionales por línea (F3.1 §3.1,
-- decisión D-1): proyecto, etapa, cliente, proveedor, cuenta bancaria
-- (destino de fondos). Multimoneda por línea: moneda + tipo_cambio +
-- importe_original; debe/haber son siempre el importe ya convertido a ARS.
--
-- No se agrega una CHECK de "debe XOR haber" a nivel de fila: los
-- borradores pueden guardarse incompletos (F3.1 §3.5) con una línea en
-- blanco todavía sin cargar. Esa regla la valida AsientoService al
-- confirmar (LINEA_DEBE_XOR_HABER). Sí se agregan las CHECK que nunca
-- bloquean un borrador (no negatividad).

CREATE TABLE secuencia (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    nombre           VARCHAR(40)  NOT NULL,
    valor_actual     BIGINT       NOT NULL DEFAULT 0,
    creado_en        DATETIME(6)  NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)  NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_secuencia_tenant_nombre UNIQUE (tenant_id, nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO secuencia (tenant_id, nombre, valor_actual, creado_en, creado_por, actualizado_en, actualizado_por, version)
VALUES (1, 'ASIENTO', 0, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);

CREATE TABLE asiento (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT        NOT NULL,
    fecha            DATE          NOT NULL,
    descripcion      VARCHAR(500)  NOT NULL,
    estado           VARCHAR(20)   NOT NULL,
    numero           BIGINT,
    origen           VARCHAR(30)   NOT NULL,
    origen_tipo      VARCHAR(60),
    origen_id        BIGINT,
    observaciones    VARCHAR(2000),
    creado_en        DATETIME(6)   NOT NULL,
    creado_por       VARCHAR(120),
    actualizado_en   DATETIME(6)   NOT NULL,
    actualizado_por  VARCHAR(120),
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_asiento_tenant_numero UNIQUE (tenant_id, numero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_asiento_tenant_fecha ON asiento (tenant_id, fecha);

CREATE TABLE asiento_linea (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    asiento_id          BIGINT        NOT NULL,
    orden               INT           NOT NULL,
    cuenta_contable_id  BIGINT        NOT NULL,
    debe                DECIMAL(18,2) NOT NULL DEFAULT 0,
    haber               DECIMAL(18,2) NOT NULL DEFAULT 0,
    moneda_id           BIGINT        NOT NULL,
    tipo_cambio         DECIMAL(19,6),
    importe_original    DECIMAL(18,2),
    fuente_tc           VARCHAR(20),
    leyenda             VARCHAR(500),
    proyecto_id         BIGINT,
    etapa_id            BIGINT,
    cliente_id          BIGINT,
    proveedor_id        BIGINT,
    cuenta_bancaria_id  BIGINT,
    generada_auto       BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en           DATETIME(6)   NOT NULL,
    creado_por          VARCHAR(120),
    actualizado_en      DATETIME(6)   NOT NULL,
    actualizado_por     VARCHAR(120),
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_asiento_linea_asiento FOREIGN KEY (asiento_id) REFERENCES asiento(id) ON DELETE CASCADE,
    CONSTRAINT fk_asiento_linea_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id),
    CONSTRAINT fk_asiento_linea_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_asiento_linea_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_asiento_linea_etapa FOREIGN KEY (etapa_id) REFERENCES etapa(id),
    CONSTRAINT fk_asiento_linea_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_asiento_linea_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_asiento_linea_cuenta_bancaria FOREIGN KEY (cuenta_bancaria_id) REFERENCES cuenta_bancaria(id),
    CONSTRAINT chk_asiento_linea_debe_haber CHECK (debe >= 0 AND haber >= 0),
    CONSTRAINT chk_asiento_linea_tc CHECK (tipo_cambio IS NULL OR tipo_cambio > 0),
    CONSTRAINT chk_asiento_linea_importe CHECK (importe_original IS NULL OR importe_original >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_asiento_linea_asiento ON asiento_linea (asiento_id);
CREATE INDEX ix_asiento_linea_cuenta ON asiento_linea (cuenta_contable_id);
