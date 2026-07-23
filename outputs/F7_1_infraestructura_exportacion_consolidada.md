# F7.1 — Infraestructura de exportación consolidada

**Paso:** 35 de 55 · **Fase:** F7 — Reportes y dashboard · **Modelo:** Sonnet 5 · **Depende de:** F3.6
**Checkpoint humano:** No.

## Qué se hizo

Consolidar `ReportExportService` (molde PL-3, iniciado en F1.8/F3.6) en un único servicio de exportación con estilos corporativos, formato AR y streaming real para volúmenes grandes, y refactorizar los 5 exports existentes para que lo usen.

### Estilos corporativos

Encabezado en Excel y PDF con: nombre de empresa (`Tenant.nombre`), título del reporte, filtros aplicados (solo los que efectivamente se usaron, ej. `"Desde: 01/03/2026"`), fecha de emisión, y numeración de páginas en PDF (`"Página X de Y"`, con el total resuelto recién al cerrar el documento vía `PdfTemplate`, que es la técnica estándar de OpenPDF/iText para este caso).

### Slot de logo configurable

`Tenant.logoClasspath` (nuevo, nullable, migración V31): ruta de un recurso ya empaquetado en el classpath. El logo en sí "llegará después" (pedido explícito del paso); mientras tanto queda en `null` y el servicio omite la imagen sin romper nada — probado en el E2E apuntando a un recurso inexistente. `GET/PUT /api/v1/tenant` (PUT solo admin, mismo criterio que `ConfiguracionAtribucion` de F6.3) deja el nombre y el logo configurables sin tocar la base de datos a mano.

### Formato AR

`FormatoReporte`: moneda `$ 1.234.567,89` (negativos como `-$ ...`) y fechas `dd/MM/yyyy`, sin depender del locale de la máquina donde se abra el archivo (antes cada controller hacía `.toString()` sobre `BigDecimal`/`LocalDate`, dando formato ISO/US). `ReportExportService` despacha por tipo de celda: `BigDecimal` → texto formateado (both Excel y PDF, para garantizar el formato visual exacto sin depender de que Excel abra con la config regional AR), `LocalDate` → celda de fecha nativa en Excel (ordenable/filtrable) con máscara `dd/mm/yyyy`, `Boolean` → `Sí/No` en PDF.

### Streaming real para volúmenes grandes

- Excel: `SXSSFWorkbook` (ya existía, sin cambios — solo mantiene 100 filas en memoria).
- **PDF (el problema real)**: la implementación anterior armaba una única `PdfPTable` completa en memoria antes de escribirla — con decenas de miles de filas, cada `PdfPCell`/`Phrase` quedaba retenido hasta el final. Se aplicó la técnica estándar de OpenPDF/iText para tablas grandes: cada 500 filas se hace `documento.add(tabla)` (flush real a la salida) seguido de `tabla.deleteBodyRows()` (libera las filas ya escritas, conserva el encabezado por `setHeaderRows(1)`). El encabezado se repite automáticamente en cada página nueva.

### API consolidada

`ContextoReporte.de(titulo, filtro1, filtro2, ...)` (varargs, descarta los `null` — cada controller arma su lista de filtros aplicados sin chequear él mismo cuáles vinieron). `nombreEmpresa`/logo se resuelven **dentro** del servicio vía `TenantRepository` + `TenantContext`, no los pasa cada caller.

### Refactor de los 5 exports existentes

`MayorController` (F3.6), `CuentaPorCobrarController`/`CuentaPorPagarController` (F4.5), `ReporteMonedasController` (molde de referencia), `ImportacionFacturaController` (rechazos, F4.6) — todos migrados a `ContextoReporte`, con sus filtros aplicados armados a partir de los query params ya recibidos, y pasando `LocalDate`/`BigDecimal` crudos en vez de pre-convertirlos a `String`.

### Bug real corregido de paso: truncado silencioso en 1000 filas

`ReporteMonedasController` usaba `PageRequest.of(0, 1000)` tanto para la pantalla como para el export — si hubiera más de 1000 monedas activas, el export se truncaba sin avisar (justo lo opuesto a lo que pide este paso). Se separó: la pantalla sigue igual, el export ahora usa `Pageable.unpaged()`.

## Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **431/432 en verde** (el único fallo es el de Testcontainers/Docker Desktop local, esperado). **14 nuevos**: `FormatoReporteTest` (5), `ReportExportServiceTest` (9, incluye export de 50.000 filas en Excel y PDF sin excepción y PDF real con >100 páginas generadas por el flush periódico).
- **E2E contra MySQL 8 real** (docker-compose): `GET/PUT /api/v1/tenant` OK; export Excel+PDF de Reporte de Monedas, Cuentas por Cobrar (0 filas, no rompe) y Mayor de una cuenta real — los 6 archivos con contenido válido (`file` confirma tipo Excel 2007+/PDF 1.5). Logo apuntando a un recurso inexistente no rompe el export (comportamiento diseñado).
- Sin cambios de frontend: los endpoints de exportación devuelven el mismo tipo de respuesta (blob), así que las páginas existentes siguen funcionando sin tocarlas.
