-- F10.3 — el EECC da "Deudores por Servicios Prestados" como un monto único
-- (5.396.077,62 al 31/08/2025), pero 1.1.2004 es una cuenta madre en el plan
-- de cuentas (agrupa 3 sub-cuentas por cliente: Valvecchia Gerardo, Centro
-- de Ojos Quilmes S.A., Kakaroto M4) y no puede recibir movimientos
-- directos. Sin el desglose por cliente del EECC, se crea una sub-cuenta
-- nueva para el saldo de apertura sin discriminar (se puede reclasificar a
-- mano por cliente más adelante si hace falta).
INSERT INTO cuenta_contable (tenant_id, codigo, nombre, padre_id, naturaleza, rubro_id, imputable, saldo_esperado, activo, creado_en, creado_por, actualizado_en, actualizado_por, version)
SELECT 1, '1.1.2004.04', 'Deudores por servicios prestados - Apertura sin discriminar por cliente',
       p.id, 'ACTIVO', p.rubro_id, TRUE, 'DEUDOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0
FROM cuenta_contable p WHERE p.tenant_id = 1 AND p.codigo = '1.1.2004';
