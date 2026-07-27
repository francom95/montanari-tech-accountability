-- F10.2: la migración histórica de cuotas de proyecto (Excel) no siempre trae fecha de cobro pactada.
ALTER TABLE proyecto_cuota MODIFY fecha_estimada_cobro DATE NULL;
