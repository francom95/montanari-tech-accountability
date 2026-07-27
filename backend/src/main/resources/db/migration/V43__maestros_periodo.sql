-- F9.3 — Períodos contables y cierre. El cierre es un control, no una
-- traba por defecto: consultar/importar/exportar nunca se bloquean, solo
-- crear/editar/anular con fecha dentro de un período cerrado. Las filas
-- nacen on-demand (motor idempotente, mismo molde que F8.1 vencimientos):
-- ausencia de fila para un (año, mes) se trata como período ABIERTO
-- implícito (ver PeriodoService.estaCerrado).
CREATE TABLE periodo (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    anio              INT          NOT NULL,
    mes               INT          NOT NULL,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'ABIERTO',
    motivo_cierre     VARCHAR(500),
    motivo_reapertura VARCHAR(500),
    creado_en         DATETIME(6)  NOT NULL,
    creado_por        VARCHAR(120),
    actualizado_en    DATETIME(6)  NOT NULL,
    actualizado_por   VARCHAR(120),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_periodo_tenant_anio_mes UNIQUE (tenant_id, anio, mes)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_periodo_estado ON periodo (tenant_id, estado);
