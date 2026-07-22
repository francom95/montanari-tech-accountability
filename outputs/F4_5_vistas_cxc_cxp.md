# F4.5 — Vistas de cuentas por cobrar y por pagar

**Paso:** 26 de 55 · **Fase:** F4 — Facturación, cobros y pagos · **Modelo asignado:** Haiku 4.5 (ejecutado con Sonnet 5 por decisión explícita del usuario) · **Depende de:** F4.4 · **Plantillas:** PL-3
**Checkpoint humano:** No.

## Nota sobre el modelo

Este paso está marcado en el plan para ejecutarse con Claude Haiku 4.5 ("paso de volumen sobre plantilla, no tomes decisiones de diseño"). Se le preguntó al usuario si prefería cambiar de modelo y decidió explícitamente continuar con Sonnet 5. Se siguió igual el espíritu del paso: replicar el molde de referencia (`MayorController`/`MayorService` de F3.6) exactamente, sin rediseñar nada, y reportar cualquier cosa que no encajara en la plantilla en vez de improvisar — ver §"Decisiones" más abajo para los dos puntos donde el molde no alcanzaba tal cual.

## Qué se hizo

Dos vistas de solo lectura (PL-3) sobre datos ya calculados por F4.4: **Cuentas por cobrar** (facturas de venta confirmadas con saldo pendiente) y **Cuentas por pagar** (facturas de compra confirmadas con saldo pendiente). Ambas reutilizan `ReportExportService` (Apache POI + OpenPDF) tal cual lo dejó F1.8, siguiendo el mismo patrón de `MayorController`/`MayorService` (F3.6): un endpoint JSON para pantalla + dos de exportación (Excel/PDF) sobre el mismo cálculo.

- **"No recalcules saldos: consumí los servicios de F4.4"** (nota explícita del paso): la vista **no reimplementa** el cálculo de saldo — usa exactamente los mismos repositorios que ya construyó F4.4 (`CobroImputacionRepository`, `AplicacionAnticipoClienteRepository`, y sus gemelos de compra), solo que **agregados en bloque para todas las facturas del reporte en una consulta cada uno**, en vez de llamar a `CobroService.saldoFacturaVenta`/`PagoService.saldoFacturaCompra` una vez por factura. Se agregaron dos métodos `sumarImputadoPorFactura`/`sumarAplicacionesPorFactura` (proyección JPQL `SELECT new ...` agrupada por factura) a esos mismos repositorios — mismo dato, mismas tablas, sin N+1.
- **Solo se listan facturas con saldo pendiente** (`saldo > 0`): "cuentas por cobrar/pagar" son las que todavía se deben, no el historial completo de facturación (eso ya lo cubre F4.2/F4.3).
- **Filtro de vencimiento** (pedido explícito del paso, además de los filtros estándar de PL-3): `VENCIDO` (vencimiento pasado), `POR_VENCER` (vencimiento futuro), `SIN_VENCIMIENTO` (factura sin fecha de vencimiento cargada).
- **Totales al pie por moneda** (pedido explícito del paso): agregado en el mismo cálculo, sin otra consulta.

## Decisiones tomadas donde el molde de referencia no alcanzaba tal cual

El paso pide "si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar" — dos puntos donde el molde de `MayorController`/`mayor-page.tsx` no cubría exactamente lo que pedía F4.5, documentados en vez de decididos en silencio:

1. **Sin paginación.** `MayorController`/`MayorService` calculan el resultado completo una sola vez y lo paginan en memoria para pantalla (porque un mayor contable puede tener miles de líneas). Cuentas por cobrar/pagar son, por diseño, un listado acotado a **facturas con saldo pendiente** — en la práctica, decenas, no miles. Se optó por devolver la lista completa sin paginar (ni en el JSON de pantalla ni en el export), evitando la complejidad de `paginar()` donde no aporta nada. Si el volumen de facturas pendientes creciera mucho en el futuro, agregar paginación es un cambio aislado al servicio, sin tocar el resto.
2. **N+1 al reusar los servicios de F4.4 tal cual.** `CobroService.saldoFacturaVenta`/`PagoService.saldoFacturaCompra` toman un solo `facturaId` — llamarlos una vez por fila del reporte (y, peor, una vez por cada factura confirmada al exportar sin paginar) sería exactamente el antipatrón que `ReportExportService` (streaming, POI `SXSSFWorkbook`) está pensado para evitar. Se resolvió agregando dos consultas nuevas por-lote a los mismos repositorios de F4.4 (`sumarImputadoPorFactura`/`sumarAplicacionesPorFactura`), no un concepto de saldo nuevo — se sigue sumando exactamente los mismos `CobroImputacion`/`AplicacionAnticipoCliente` que ya construyó F4.4, solo agrupados por factura en una consulta en vez de N.

## Verificación realizada

- **Compilación** dentro de contenedor `maven:3.9-eclipse-temurin-21`: limpia.
- **9 tests nuevos**: `CuentaPorCobrarServiceTest` (sin facturas, saldo parcial correcto, factura totalmente cobrada no aparece, combina imputaciones de cobro directo + aplicaciones de anticipo, filtro vencido, totales por moneda), `CuentaPorPagarServiceTest` (mismos casos, simétrico).
- **254/255 tests** en la suite completa (el único que falla es el Testcontainers/Docker Desktop local ya conocido, ver [[build-env-jdk21-testcontainers]]).
- **Verificación end-to-end contra el servidor real** (migraciones V1→V22 sin cambios, ya que F4.5 no agrega tablas):
  - 3 facturas de venta: una cobrada en su totalidad (10.000), una vencida sin cobrar (20.000), una por vencer sin cobrar (30.000). `GET /reportes/cuentas-por-cobrar` sin filtros devuelve exactamente las 2 con saldo pendiente (la cobrada NO aparece) y `totalesPorMoneda` = 50.000 ARS.
  - Filtro `estadoVencimiento=VENCIDO` devuelve solo la factura vencida, con su propio total (20.000).
  - `GET /reportes/cuentas-por-cobrar/exportar/excel` y `/exportar/pdf` devuelven archivos reales (`.xlsx`/`.pdf` válidos, verificado con `file`), 200 OK.
  - Cuentas por pagar: mismo comportamiento verificado con una factura de compra vencida.
  - Permisos: usuario `LECTURA` real recibe 200 tanto en la consulta como en la exportación (son reportes de solo lectura, sin restricción de rol — coherente con que el resto de los reportes del sistema, como el Mayor, tampoco la tienen).
- Frontend: `tsc -b` y `oxlint` limpios sobre `cuentas-por-cobrar-page.tsx`, `cuentas-por-pagar-page.tsx` y el wiring de router/nav (solo los 2 warnings preexistentes en componentes UI compartidos, no relacionados).
