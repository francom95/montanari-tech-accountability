# F5.2 — Parsers de resúmenes (Galicia, Mercado Pago, tarjeta)

**Paso:** 29 de 55 · **Fase:** F5 — Bancos y conciliación · **Modelo:** Sonnet 5 · **Depende de:** F5.1
**Checkpoint humano:** Sí — verificar con un resumen real de cada origen. **Ver sección final: pendiente de confirmación explícita del contador.**

## Qué se hizo

Arquitectura de parsers por estrategia (`ResumenParser` + `MovimientoParseado`), con 3 implementaciones — `ParserGalicia` (Excel), `ParserMercadoPago` (Excel), `ParserTarjeta` (PDF) — que **nunca persisten nada**: solo normalizan el archivo real a filas candidatas. `ImportacionMovimientoBancarioService` resuelve lo que el archivo no declara (moneda, según la cuenta bancaria destino elegida), detecta duplicados por hash y recién ahí llama a `MovimientoBancarioService.crear` (F5.1) — cada fila entra como `MovimientoBancario` **PENDIENTE**, igual que la carga manual.

Se probó contra los 5 archivos reales provistos (Galicia ARS.xlsx, Galicia USD.xlsx, Resumen de Cuenta.xlsx de Mercado Pago, VISA Business.pdf) más un certificado de retenciones que se determinó fuera de alcance (ver más abajo).

### Cambio de infraestructura en F5.1: `fecha` pasa a nullable

El archivo real Galicia ARS.xlsx no trae fecha en **ninguna** de sus 32 filas (columna vacía, confirmado con `openpyxl` contra el archivo real). El usuario decidió explícitamente (ver memoria `f52-galicia-ars-fecha-decision`, antes de implementar): el parser deja `fecha = null` y el movimiento entra igual a la bandeja de F5.1, a completar con "corregir" antes de poder confirmar/imputar — mismo patrón que la carga asistida de F4.6. Esto requirió:
- Migración V24: `movimiento_bancario.fecha` pasa a `NULL` permitido.
- `MovimientoBancarioService.confirmar`/`imputar` ahora exigen fecha (`FECHA_PENDIENTE` si falta) antes de generar el asiento — `asociar`/`descartar` no la necesitan.
- Frontend F5.1 (`movimientos-bancarios-page.tsx`): fila sin fecha muestra "Sin fecha", botones Confirmar/Imputar deshabilitados hasta completarla con "Editar".

### Duplicados: hash + columna nueva

`hash_importacion` (nuevo, `movimiento_bancario`) + índice único `(cuenta_bancaria_id, hash_importacion)`. El hash lo calcula el **servicio de importación** (no el parser, que no conoce la cuenta destino): SHA-256 de cuenta+fecha+importe+descripción+referencia. Re-importar el mismo archivo marca todas las filas como `DUPLICADO` sin re-crear nada (verificado en vivo).

### Galicia (`ParserGalicia`)

Hoja "Movimientos" (16 columnas), búsqueda de columnas por nombre de encabezado (no por posición fija) para tolerar variaciones. `importe` = Créditos si > 0, si no −Débitos. `monedaCodigo` sale **siempre nulo**: el archivo (ARS o USD) no declara su propia moneda en ninguna fila — la resuelve el servicio de importación usando la moneda de la cuenta bancaria elegida al importar, igual para todo el archivo.

**Bug real encontrado solo contra el archivo real** (no lo reproduce un workbook armado con el writer de POI): Galicia USD.xlsx usa celdas de fecha con `t="d"` (ISO 8601, `<v>2026-06-02T00:00:00.000Z</v>`) en vez del serial numérico tradicional de Excel — un tipo de celda que el enum `ST_CellType` de Apache POI/xmlbeans no reconoce (confirmado contra POI 5.3.0, 5.4.1 y 5.5.1 — no es un problema de versión, quedó en 5.3.0). Cualquier `getCellType()` sobre esa celda específica revienta con `XmlValueOutOfRangeException`. Fix: `fechaDeCelda` atrapa esa excepción puntual y cae a `XSSFCell.getRawValue()` (devuelve el texto crudo de `<v>` sin pasar por la validación de tipo rota), parseado como instante ISO 8601. Test de regresión: un .xlsx armado a mano byte a byte con esa celda exacta (POI no puede generar `t="d"` con su propio writer, así que no hay otra forma de reproducirlo en un test).

### Mercado Pago (`ParserMercadoPago`)

"Resumen de Cuenta.xlsx", hoja "sheet0": todas las celdas son texto (a diferencia de Galicia), incluye separador decimal AR (coma) parseado a mano. Bloque 1 (`INITIAL_BALANCE`/`CREDITS`/`DEBITS`/`FINAL_BALANCE`, resumen en inglés) se ignora; bloque 2 (`RELEASE_DATE`/`TRANSACTION_TYPE`/`REFERENCE_ID`/`TRANSACTION_NET_AMOUNT`) es el detalle real. El archivo provisto solo trae **un** movimiento real (un crédito) — no hay ejemplo de débito para confirmar el signo con certeza; el parser confía en un "−" explícito en el texto y por defecto asume ingreso, coherente con el único caso real disponible. **Punto a confirmar en el checkpoint** si aparecen más resúmenes con débitos.

### Tarjeta (`ParserTarjeta`, PDF)

PDFBox básico (`setSortByPosition(false)`, igual que `ExtractorFacturaPdf` de F4.6) sobre el resumen VISA Business (Banco Galicia). **No es un parser estructural de layout**: todas las filas de movimiento (bloque CONSOLIDADO y DETALLE DEL CONSUMO + impuestos/comisiones) empiezan con una fecha `dd-MM-yy` — eso alcanza para filtrar encabezados/pies de página repetidos por página, el subtotal "TARJETA ... Total Consumos de ..." y el "TOTAL A PAGAR" final, que nunca empiezan con fecha.

Verificado contra el texto REAL capturado con un test temporal antes de escribir el parser (mismo método que F4.6): 28 movimientos reales, sin discrepancias entre el test con texto hardcodeado y la ejecución contra el PDF real.

**Decisión de alcance, no confirmada con la contadora todavía**: el resumen mezcla 2 tipos de filas muy distintos —
- Bloque CONSOLIDADO (SU PAGO EN PESOS/USD, DEV.IMP RG 5617): son los movimientos que realmente mueven la cuenta bancaria (`CuentaBancaria.cuentaContable`, F2.4/F4.4).
- DETALLE DEL CONSUMO + impuestos/comisiones: consumos individuales del período, que incrementan la deuda de la tarjeta pero **no mueven la cuenta bancaria en el momento** — F5.4 ("tarjetas de crédito: operatoria completa") es el paso que trae el modelo propio (`TarjetaCredito`, ya existe como maestro desde antes, con su propia `cuentaBancariaDebito`) para esta granularidad.

En vez de decidir unilateralmente cuáles de las 28 filas importar, se agregó un checkbox "Importar" por fila en el frontend (todas tildadas por defecto salvo duplicados) — el usuario elige en el momento qué entra a la bandeja. Verificado en vivo: importar solo las 3 filas de CONSOLIDADO funciona correctamente.

## Verificación realizada

- **Compilación y suite completa** en contenedor `maven:3.9-eclipse-temurin-21`: 305/306 (el único que falla es el Testcontainers/Docker Desktop local ya conocido). 4 tests nuevos de `ParserGaliciaTest` (incluye el de la celda `t="d"`), 2 de `ParserMercadoPagoTest`, 8 de `ParserTarjetaTest`, 10 de `ImportacionMovimientoBancarioServiceTest`, 2 nuevos en `MovimientoBancarioServiceTest` (fecha nula, `FECHA_PENDIENTE`).
- **Verificación end-to-end contra los 5 archivos reales** vía docker-compose (migraciones V1→V24 desde volumen limpio):
  - Galicia ARS.xlsx (32 filas, cuenta Banco Galicia CC): previsualizar y confirmar → 32 `IMPORTADO`, todas sin fecha. Re-previsualizar/re-confirmar el mismo archivo → 32 `DUPLICADO`, no se re-crea nada.
  - Bandeja: movimiento sin fecha rechaza `imputar` con `FECHA_PENDIENTE`; `corregir` completa la fecha; `imputar` con una cuenta imputable real genera el asiento y pasa a `CONCILIADO`.
  - Galicia USD.xlsx (5 filas, cuenta Banco Galicia USD): previsualizar confirma el fix de la celda `t="d"` (fechas correctas); confirmar sin `tipoCambioUsd` → 5 `ERROR` (`TC_REQUERIDO`); confirmar con TC=1200 → 5 `IMPORTADO`, `importeArs` calculado correctamente.
  - Resumen de Cuenta.xlsx (Mercado Pago, cuenta Mercado Pago): el único movimiento real → `IMPORTADO`.
  - VISA Business.pdf (cuenta Banco Galicia CC): previsualizar → 28 filas, monedas ARS/USD mezcladas correctamente por fila; confirmar solo las 3 filas de CONSOLIDADO (simulando destildar el detalle de consumos) → 3 `IMPORTADO`.
  - Permisos: usuario `LECTURA` real recibe 403 al previsualizar/confirmar, 200 al leer la bandeja.
- **Bug real corregido durante la verificación** (no detectado por los tests con datos sintéticos): `BigDecimal.valueOf(double)` para montos ≥ 10.000.000 (ej. "Deposito En Efectivo" 14.620.000,00 de Galicia ARS.xlsx) serializaba en notación científica (`1.462E+7`) por cómo `Double.toString` construye el `BigDecimal` — se normalizó a 2 decimales en `ParserGalicia.numeroDeCelda`.
- Frontend: `tsc -b` limpio, `oxlint` limpio sobre los archivos nuevos y modificados.

## Checkpoint humano — pendiente de confirmación explícita

⚠️ Este paso trae **2 puntos concretos** que necesitan validación del contador con más resúmenes reales, no solo los 5 provistos:
1. **Mercado Pago**: el único movimiento disponible es un crédito sin signo explícito — falta un ejemplo de débito para confirmar que el criterio "confía en el '−' del texto, default ingreso" es correcto.
2. **Tarjeta**: confirmar que separar CONSOLIDADO (bandeja F5.2/F5.1) de DETALLE DEL CONSUMO (futuro F5.4) es el criterio correcto, y no que todo el resumen deba entrar a algún lado distinto.

Además, sigue pendiente el checkpoint humano de F4.4 (caso CO-2, cobro USD con diferencia de cambio) — nunca confirmado por el contador.
