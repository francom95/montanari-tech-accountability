# F7.6 — Exportaciones restantes en lote

**Paso:** 40 de 55 · **Fase:** F7 — Reportes y dashboard · **Modelo asignado:** Haiku 4.5 (ejecutado con Sonnet 5 por elección del usuario) · **Depende de:** F7.1, F6.2
**Checkpoint humano:** No.

## Qué se hizo

Paso de volumen sobre plantilla: aplicar el molde PL-3 (`ReportExportService` de F7.1) a los 5 listados de la sección 7.9 que todavía no tenían exportación — **clientes**, **proveedores**, **movimientos contables (libro diario)**, **liquidación de IVA**, **liquidación de IIBB** — sin crear pantallas nuevas ni tomar decisiones de diseño. Cada uno replica exactamente la estructura ya usada por `CuentaPorCobrarController` (F4.5) y `MayorController` (F3.6/F7.2): constante `COLUMNAS`, dos endpoints `GET .../exportar/excel` y `GET .../exportar/pdf` con los mismos filtros que el endpoint de listado (forzando `Pageable.unpaged()`), un `aFilas(...)` privado y un `contexto(...)` privado que arma el `ContextoReporte` con los filtros aplicados como texto.

En el frontend, cada hook ganó un par `descargar<Entidad>Excel/Pdf` (blob + `<a download>`, mismo patrón que `use-cuenta-por-cobrar.ts`) y cada página ganó dos botones "Exportar Excel"/"Exportar PDF" junto al título o al header de la tabla, con un estado local `descargando` para deshabilitar los botones durante la descarga — sin tocar ninguna otra funcionalidad de las pantallas existentes.

### Sin decisiones de diseño nuevas

Tal como pedía el plan, no hubo ambigüedad que resolver: las columnas de cada exportación son exactamente las que ya mostraba la tabla en pantalla (Clientes/Proveedores: sus columnas de listado; Libro diario: N°/Fecha/Descripción/Estado/Origen/Debe/Haber, igual que `asientos-page.tsx`; IVA/IIBB: Período/Estado/A pagar/(Saldo técnico o A favor)/Asiento, igual que la tabla "Liquidaciones de {año}" de cada pantalla). Movimientos contables se confirmó que es el libro diario (`AsientoController`), distinto del Mayor por cuenta (`MayorController`, ya exportable desde F7.2).

## Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **482/483 en verde** (único fallo esperado: Testcontainers/Docker Desktop local, mismo que en pasos anteriores). Sin tests nuevos: ni `CuentaPorCobrarController` ni `MayorController` (los dos exports de referencia ya existentes) tienen tests a nivel controller/HTTP — no hay una plantilla de test de export que replicar, y agregar una para este paso puntual sería alcance nuevo fuera de "solo replicar la plantilla".
- Frontend: `tsc -b` y `oxlint` limpios (mismos 2 warnings preexistentes de siempre, ninguno nuevo).

### E2E contra MySQL 8 real (docker-compose)

Se probaron los 10 endpoints nuevos (Excel + PDF × 5 pantallas) contra el backend dockerizado:

| Endpoint | Excel | PDF |
|---|---|---|
| `/clientes/exportar/*` | 200, firma ZIP válida | 200, firma `%PDF` válida |
| `/proveedores/exportar/*` | 200, firma ZIP válida | 200, firma `%PDF` válida |
| `/asientos/exportar/*` | 200, firma ZIP válida | 200, firma `%PDF` válida |
| `/impuestos/liquidaciones-iva/exportar/*` | 200, firma ZIP válida | 200, firma `%PDF` válida |
| `/impuestos/liquidaciones-iibb/exportar/*` | 200, firma ZIP válida | 200, firma `%PDF` válida |

Con un cliente real cargado (`Cliente E2E F7.6`, CUIT `20-12345678-6`, jurisdicción CABA, contacto Juan Test), se desempaquetó el `.xlsx` generado y se confirmó que el CUIT, nombre, jurisdicción, contacto y estado aparecen correctamente en la hoja — probando que el mapeo de filas (`aFilas`) funciona con datos reales, no solo en el caso vacío.

Se confirmó contra la lista de la sección 7.9 que los 11 exports esperados (todos salvo flujo de caja, que llega con F8.3) existen: mayores, balance, ER, por proyecto, CxC, CxP, IVA, IIBB, clientes, proveedores, movimientos.

### Incidente de infraestructura durante la verificación (no relacionado al código)

El primer intento de build de la imagen Docker falló dos veces por cortes de red transitorios de Maven Central (paquetes truncados a mitad de descarga); al reintentar sin cambios, el build fue exitoso. Además, un comando propio con `&&` a través de un pipe a `tail` enmascaró un fallo de build anterior y dejó un volumen de MySQL con una migración a medio aplicar (V34, ya probada muchas veces en pasos anteriores) — se resolvió con `docker compose down -v` y un `up -d` limpio. Ninguno de los dos problemas es una regresión de F7.6.
