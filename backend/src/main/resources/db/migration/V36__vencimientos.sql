-- F8.1 — Calendario de vencimientos.

-- 1) Concepto.periodicidad pasa de texto libre (nunca antes leído por
-- ninguna lógica) a un enum fijo: VencimientoService lo usa para decidir
-- si generar un vencimiento del mes/año en curso. Se normalizan los
-- valores existentes por palabra clave antes de fijar el tipo de columna;
-- cualquier valor no reconocido (incluido NULL) cae a UNICA (no recurrente,
-- mismo comportamiento práctico que "sin periodicidad" antes de este paso).
UPDATE concepto_recurrente SET periodicidad = 'MENSUAL' WHERE periodicidad IS NOT NULL AND LOWER(periodicidad) LIKE '%mensual%';
UPDATE concepto_recurrente SET periodicidad = 'ANUAL' WHERE periodicidad IS NOT NULL AND LOWER(periodicidad) LIKE '%anual%';
UPDATE concepto_recurrente SET periodicidad = 'UNICA' WHERE periodicidad IS NULL OR periodicidad NOT IN ('MENSUAL', 'ANUAL');
ALTER TABLE concepto_recurrente MODIFY COLUMN periodicidad VARCHAR(20) NOT NULL DEFAULT 'UNICA';

-- 2) Vencimiento: obligación de pago futura, generable a mano o
-- automáticamente desde liquidaciones de IVA/IIBB confirmadas, tarjetas de
-- crédito y conceptos recurrentes (ver VencimientoService.generarAutomaticos,
-- disparado on-demand por el frontend — el proyecto no tiene infraestructura
-- de scheduling y este paso no la introduce). estado_efectivo VENCIDO se
-- calcula en lectura (fecha < hoy && estado = PENDIENTE), nunca se persiste
-- (mismo criterio que EstadoVencimiento de F4.5).
CREATE TABLE vencimiento (
    id                            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                     BIGINT        NOT NULL,
    descripcion                   VARCHAR(200)  NOT NULL,
    tipo                          VARCHAR(30)   NOT NULL,
    fecha                         DATE          NOT NULL,
    importe_estimado              DECIMAL(18,2),
    moneda_id                     BIGINT        NOT NULL,
    recurrencia                   VARCHAR(20)   NOT NULL,
    intervalo_dias_personalizado  INT,
    estado                        VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    cuenta_contable_id            BIGINT,
    proveedor_id                  BIGINT,
    liquidacion_tipo               VARCHAR(10),
    liquidacion_id                 BIGINT,
    tarjeta_credito_id             BIGINT,
    proyecto_id                    BIGINT,
    concepto_recurrente_id         BIGINT,
    asiento_vinculado_id           BIGINT,
    origen_generacion              VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    origen_generacion_ref_id       BIGINT,
    observaciones                  VARCHAR(500),
    motivo_cancelacion             VARCHAR(500),
    creado_en                      DATETIME(6)  NOT NULL,
    creado_por                     VARCHAR(120),
    actualizado_en                 DATETIME(6)  NOT NULL,
    actualizado_por                VARCHAR(120),
    version                        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_vencimiento_moneda FOREIGN KEY (moneda_id) REFERENCES moneda(id),
    CONSTRAINT fk_vencimiento_cuenta_contable FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id),
    CONSTRAINT fk_vencimiento_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_vencimiento_tarjeta_credito FOREIGN KEY (tarjeta_credito_id) REFERENCES tarjeta_credito(id),
    CONSTRAINT fk_vencimiento_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecto(id),
    CONSTRAINT fk_vencimiento_concepto_recurrente FOREIGN KEY (concepto_recurrente_id) REFERENCES concepto_recurrente(id),
    CONSTRAINT fk_vencimiento_asiento_vinculado FOREIGN KEY (asiento_vinculado_id) REFERENCES asiento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_vencimiento_fecha ON vencimiento (tenant_id, fecha);
CREATE INDEX ix_vencimiento_estado ON vencimiento (tenant_id, estado);
CREATE INDEX ix_vencimiento_origen ON vencimiento (tenant_id, origen_generacion, origen_generacion_ref_id);
