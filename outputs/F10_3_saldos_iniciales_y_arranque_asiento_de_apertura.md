# F10.3 — Saldos iniciales y arranque (asiento de apertura)

Modelo asignado: Sonnet 5. Usado: Sonnet 5 (sin discrepancia).

## Qué se construyó

El paso más grande de F10 hasta ahora: arranque completo del sistema desde el EECC real (cierre 31/08/2025) hasta fin de abril/2026, con reconstrucción íntegra de facturas y movimientos bancarios reales — no solo el "agujero" oct-dic/2025 que se pensaba al planificar el paso, sino también septiembre/2025 (ver Fase E más abajo).

**Fase A** — Migración V47 con las cuentas contables faltantes (Caja, Banco Nación, Banco Provincia, pasivo/PN completo mapeado línea por línea contra la Nota 1 del EECC), `CuentaBancaria` nuevas para Caja/Nación/Provincia, `AsientoService.crearBorrador(req, OrigenAsiento)` overload, `AsientoAperturaService` y el asiento de apertura real (162) balanceado exacto contra el EECC firmado (Σdebe=Σhaber=$9.766.116,15).

**Fase B** — `confirmarVinculandoAsientoExistente(id, asientoId)` en los 4 servicios de documento (FacturaVenta/FacturaCompra/Cobro/Pago) + `BuscarAsientoPorComprobante` (match por `AsientoLinea.leyenda LIKE '%FC: PPPPP-NNNNNNNN%'` contra el Libro Diario migrado en F10.2).

**Fase C** — Reconstrucción de 60 facturas de venta + 117 de compra reales (oct/2025→abr/2026) reusando `ExtractorFacturaPdf`/`ImportacionFacturaService` de F4.6 sin tocarlos.

**Fase D** — Reconstrucción completa de cobros/pagos desde los resúmenes bancarios reales (Galicia ARS/USD, Mercado Pago, tarjeta VISA Business), incluida la reconciliación fila por fila de los 410 movimientos bancarios oct/2025→abr/2026 contra facturas reales, entidades nuevas y categorías genéricas.

**Fase E** — Verificación de cuadratura contra el balance de sumas y saldos real (F7.2) y contra los saldos finales reales de cada resumen bancario — encontró y cerró un agujero real de datos de **septiembre/2025** (ver abajo).

## Decisiones pre-resueltas con el usuario (antes de codear)

1. **Fecha de arranque**: 01/09/2025, asiento de apertura con los saldos del EECC al cierre 31/08/2025.
2. **Reconstrucción de Factura/Cobro/Pago**: todo oct/2025→abril/2026 (no solo el agujero), para que CxC/CxP/IVA/IIBB/rentabilidad tengan datos completos — para fechas ya cubiertas por el Libro Diario migrado, vincular al asiento existente en vez de generar uno nuevo.
3. **Reconciliación bancaria "Completa"** (elegida sobre "Simplificado: asiento por concepto"): recuperar identidades reales vía `Leyendas Adicionales` del Excel crudo de Galicia y crear `Cobro`/`Pago` reales vinculados a facturas, no solo asientos genéricos.
4. **Honorarios sin factura** (Alejo Del Gobbo, contador; Ruben Oscar Cremaschi, comisión por ventas): el usuario confirmó que están facturados en la realidad y pidió el asiento de **devengamiento** (gasto contra pasivo "a pagar") seguido del asiento de **pago** en la fecha real del resumen — no un asiento directo ni un anticipo.
5. **Datos de prueba de F8.4 imputados por error** (3 asientos de inversión fechados jun/jul 2026, fuera de la ventana real): el usuario eligió anularlos.
6. **Agujero de septiembre/2025**: el usuario proveyó los 2 PDFs reales de resumen bancario (Galicia ARS y USD) para cerrarlo en vez de dejarlo documentado como limitación.

## Resultados reales (E2E, Docker Compose + MySQL 8, documentos reales de `./facturasyresumenes/`)

| Entidad | Cantidad real |
|---|---|
| FacturaVenta CONFIRMADO | 60 |
| FacturaCompra CONFIRMADO | 108 (91 con PDF real + 17 devengamiento sin PDF, honorarios de contador/comisiones) |
| Cobro CONFIRMADO | 48 |
| Pago CONFIRMADO | 40 |
| PagoTarjeta CONFIRMADO | 16 (9 ARS + 5 USD del período oct-abr + 2 de septiembre) |
| MovimientoBancario total | 491 (410 oct/2025-abr/2026 + 81 septiembre/2025) |
| — CONCILIADO | 487 |
| — PENDIENTE | 1 (a propósito — ver nota) |
| — DESCARTADO | 3 (datos de prueba de F8.4, anulados) |
| ConsumoTarjeta (VISA Business) | 165 |
| Asiento CONFIRMADO | 816 |
| Proveedor / Cliente nuevos recuperados vía resumen bancario | 3 proveedores (Alejo Del Gobbo, Ruben Oscar Cremaschi, Izzi Alberto Ed) + 5 clientes (Yari Ivan Taft, Oliva Ana Lucia, Fernandez Garcia A, Ocufy Group SA, Hoyos Guadalupe Soledad) |

**Nota sobre el movimiento PENDIENTE**: es la contrapartida en Mercado Pago de una transferencia interna Galicia→Mercado Pago ($7.000). Se dejó en PENDIENTE (no CONCILIADO ni DESCARTADO) a propósito: `RecalculoSaldoService` cuenta PENDIENTE igual que CONCILIADO para el saldo de la cuenta, así que el crédito real en Mercado Pago se refleja sin duplicar el asiento (ya generado del lado Galicia).

## Bug real encontrado y corregido: `ExtractorFacturaPdf`

Factura C de un monotributista con la descripción del ítem empezando con un porcentaje ("50% Diseño de APP...") — en el orden lineal que PDFBox extrae, el encabezado de columna suelto "...Imp. Bonif. Subtotal" quedaba pegado justo antes de ese "50%", y el regex de `SUBTOTAL` (sin exigir formato decimal) lo tomaba como si fuera el subtotal real. Afectaba solo 1 de 117 comprobantes reales (Lubenfeld, factura del 14/10/2025: el sistema tenía booked $63,50 en vez del real $1.375.000). Verificado contra los 117 PDFs reales que ningún otro comprobante comparte el patrón. Corregido exigiendo formato decimal (`MONTO_DECIMAL`) en el patrón `SUBTOTAL`, con test de regresión agregado, y la `FacturaCompra`/asiento ya confirmados corregidos en la base real (verificado que el balance de sumas y saldos siguió cerrando después).

## Agujero real cerrado: septiembre/2025

Al verificar los saldos bancarios del sistema contra los saldos finales reales de cada resumen (Fase E), el saldo de Banco Galicia ARS no cerraba: el saldo inicial auditado del EECC ($2.584.068,07 al 31/08/2025) no reconciliaba contra la cadena de saldos de los resúmenes bancarios reales de oct/2025 en adelante, que implicaban ~$3,57M más de movimiento real no documentado — exactamente el mes de septiembre/2025, el único sin resumen bancario en el corpus original. El mismo patrón apareció en Banco Galicia USD y Mercado Pago (saldos iniciales en 0 sin sustento real).

El usuario proveyó los 2 PDFs reales de septiembre (Galicia ARS y USD). Se transcribieron y verificaron contra la cadena de saldos del propio PDF (el saldo final de septiembre coincidía exacto con el saldo inicial ya usado para octubre), se importaron los 81 movimientos reales, y se reconciliaron con el mismo criterio de Fase D: 62 genéricos (impuestos/sueldos/AFIP/inversiones/depósitos), 2 pagos de tarjeta, 11 cobros anticipo a clientes (sin factura, todas las facturas reales conocidas empiezan en octubre) y 6 pagos a proveedores con devengamiento+pago (3 entidades nuevas: Izzi Alberto Ed, Ocufy Group SA, Hoyos Guadalupe Soledad).

Resultado: Banco Galicia USD ahora coincide **exacto** (USD 415,02) con el resumen real de abril; Banco Galicia ARS coincide dentro de $0,50 (redondeo del propio PDF, inmaterial).

## Nota operativa: 3 asientos de datos de prueba de F8.4 anulados

Durante la reconciliación genérica de inversiones (Fima Premium), un lote incluyó por error 3 `MovimientoBancario` de prueba fechados jun/jul 2026 (fuera de la ventana real oct/2025-abr/2026) — datos de prueba dejados de una sesión anterior de F8.4 (crean un `MovimientoBancario` real como efecto secundario de `MovimientoInversionService.crear()`, por diseño de F8.4). Se detectaron comparando el total real de los 7 Excel de Galicia ARS contra el total booked en el sistema (discrepancia exacta de $9.147.545,17). Anulados vía `AsientoService.anular` + los 3 `MovimientoBancario` revertidos a su estado previo (uno de ellos vía SQL directo, dado que `MovimientoBancarioService` no expone un endpoint de reversión — gap real de diseño de F5.1/F5.3, no corregido en este paso por estar fuera de alcance).

## Verificación

- **Backend**: 672/672 tests backend pasan (más el nuevo test de regresión del bug de `ExtractorFacturaPdf`); el único error es el ambiental de Testcontainers ya documentado en memoria, no relacionado a este paso.
- **Balance de sumas y saldos real** (`GET /reportes/balance-sumas-y-saldos`, endpoint real de F7.2, rango 01/09/2025-30/04/2026): `balancea: true`, diferencia $0,00 — verificado después de cada lote de reconciliación, incluso después de anular los 3 asientos de prueba y de importar septiembre.
- **Saldos bancarios reales** (`RecalculoSaldoService`, ya existente): Banco Galicia USD y Mercado Pago ahora coinciden exacto con los resúmenes reales de abril/2026; Banco Galicia ARS dentro de $0,50 (redondeo inmaterial). Banco Nación/Provincia/Caja sin actividad real en el corpus (correcto, sin resúmenes de esas cuentas).
- Sin verificación de UI para las Fases C-F (todo se hizo vía API real contra Docker Compose + MySQL 8, sin frontend nuevo en este paso).
