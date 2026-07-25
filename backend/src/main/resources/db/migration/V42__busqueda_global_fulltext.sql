-- F9.2 — Búsqueda global "Lupita": FULLTEXT (primera vez en el proyecto) sobre
-- las columnas de texto libre que hoy solo soportan LIKE, para relevancia real
-- en la detección de término tipo TEXTO. Campos cortos (numero, cuit, codigo)
-- no llevan FULLTEXT: ft_min_word_len no aporta nada a 10-13 caracteres.
ALTER TABLE asiento ADD FULLTEXT INDEX ft_asiento_descripcion (descripcion);
ALTER TABLE movimiento_bancario ADD FULLTEXT INDEX ft_movimiento_bancario_descripcion (descripcion);
ALTER TABLE vencimiento ADD FULLTEXT INDEX ft_vencimiento_descripcion (descripcion);
ALTER TABLE pendiente_administrativo ADD FULLTEXT INDEX ft_pendiente_administrativo_titulo (titulo);
ALTER TABLE cliente ADD FULLTEXT INDEX ft_cliente_nombre (nombre);
ALTER TABLE proveedor ADD FULLTEXT INDEX ft_proveedor_nombre (nombre);
ALTER TABLE proyecto ADD FULLTEXT INDEX ft_proyecto_nombre (nombre);
ALTER TABLE etapa ADD FULLTEXT INDEX ft_etapa_nombre (nombre);
ALTER TABLE cuenta_contable ADD FULLTEXT INDEX ft_cuenta_contable_nombre (nombre);
