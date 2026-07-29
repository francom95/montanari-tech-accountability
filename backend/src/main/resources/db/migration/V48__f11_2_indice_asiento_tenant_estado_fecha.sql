-- F11.2 performance: las queries más pesadas del sistema (Mayor F3.6, Balance de
-- sumas y saldos F7.2, Estado de Resultados F7.3, liquidaciones de IVA/IIBB F6.x)
-- filtran siempre por (tenant_id, estado='CONFIRMADO', fecha BETWEEN ...) antes de
-- unir con asiento_linea. El índice existente ix_asiento_tenant_fecha no incluye
-- estado, así que no permite podar por estado antes del rango de fecha. Verificado
-- con EXPLAIN sobre los datos reales (847 asientos): a esta escala el optimizer
-- todavía elige un full scan por ser más barato que cualquier índice, pero el
-- índice queda disponible para cuando el volumen crezca.
ALTER TABLE asiento ADD INDEX ix_asiento_tenant_estado_fecha (tenant_id, estado, fecha);
