-- F9.1: umbral opcional de saldo bajo por cuenta bancaria (alerta SALDO_BAJO). Sin backfill: NULL = sin alerta.
ALTER TABLE cuenta_bancaria ADD COLUMN saldo_minimo_alerta DECIMAL(18,2) NULL AFTER saldo_actual;
