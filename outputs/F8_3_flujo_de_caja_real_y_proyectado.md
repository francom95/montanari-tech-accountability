# F8.3 — Flujo de caja real y proyectado

Modelo asignado y ejecutado: Sonnet 5 (sin discrepancia con el modelo activo de la sesión).

## Qué se construyó

Un agregador de solo lectura (paquete `flujocaja`, sin entidades propias) que consolida en una sola serie temporal en ARS lo que **ya pasó** (real, desde saldos + movimientos bancarios/tarjeta) y lo que **se espera que pase** (proyectado, desde cuotas de proyecto, compromisos, vencimientos y CxP pendientes), con detección de saldo negativo futuro. `FlujoCajaService.flujoReal(desde, hasta, granularidad)`, `flujoProyectado(dias)` y `combinado(diasAtras, diasAdelante)`, expuestos vía `FlujoCajaController` (`/api/v1/flujo-caja`) con exportación Excel/PDF (molde PL-3). Frontend: `flujo-caja-page.tsx` con gráfico de serie temporal SVG hand-rolled (no hay librería de charting en el proyecto), tabla de detalle y selector de vista/granularidad.

## Tres gaps de diseño resueltos con el usuario antes de implementar

La investigación previa (Explore agent) encontró que el plan no especificaba tres cosas necesarias para construir el motor. Se resolvieron vía `AskUserQuestion`, dos de ellas **no** con la opción recomendada:

1. **Cuotas de proyecto "pendientes" (F2.5)**: `ProyectoCuota` no tiene campo de estado. Decisión del usuario: **emparejar contra cobros confirmados** (no "por fecha", la opción recomendada) — mismo patrón ordinal que F7.4's `calcularComparacionPresupuesto`, pero contra `Cobro` en vez de `FacturaVenta` (la fecha de cobro real es lo relevante para caja, no la de facturación). Cuota N es "cobrada" si existe una Nª imputación confirmada por fecha de cobro ascendente.
2. **TC de proyección**: no existe ningún campo configurable para fechas futuras. Decisión del usuario: **última cotización cargada** (no un campo de configuración nuevo, la opción recomendada) — `TipoCambioRepository.findFirstByMonedaIdAndActivoTrueOrderByFechaDesc`, con exclusión + advertencia si no hay ninguna.
3. **"± inversión ± financiación"** de la fórmula del plan: ninguno de los dos existe hoy ("inversión" es F8.4, que depende de F8.3, no al revés; "financiación" no aparece en ningún paso futuro). Decisión del usuario: **omitir ambos términos ahora** (la opción recomendada) — fórmula real = saldo inicial + cobrado − pagado, diseñada en pasos separables para que F8.4 pueda sumar su término después.

## Diseño de conversión de moneda (minimiza aproximación)

Los montos a nivel de movimiento (`MovimientoBancario`, `ConsumoTarjeta`, `PagoTarjeta`, `Cobro`, `Pago`, `CuentaPorPagarFilaResponse.saldoArs`) ya tienen su `importeArs` histórico guardado al momento de la transacción — se usan directamente, sin conversión nueva. Solo los **saldos puntuales** (saldo inicial de cuenta/tarjeta al arrancar la ventana, o el importe de un Compromiso/Vencimiento/ProyectoCuota en moneda no-ARS) usan el fallback de última cotización cargada — la aproximación queda acotada a eso, no a cada transacción.

## Por qué el real usa movimientos bancarios, no Cobro/Pago directamente

El texto del plan ("ingresos cobrados − egresos pagados") podría leerse como sumar `Cobro.total`/`Pago.total` directamente. Se descartó: eso duplicaría contra los movimientos bancarios ya conciliados (F5.3) cuando ambos existen para la misma transacción. El real usa `MovimientoBancario` + consumos/pagos de tarjeta como fuente única — mismo criterio que `RecalculoSaldoService` (F5.4/F7.5). Confirmado en el E2E: un Cobro confirmado (accounting-side) no alteró el flujo real ya verificado, exactamente como se esperaba.

## Verificación

- **Backend**: 522 tests, 0 fallas propias (Testcontainers ambiental aparte). 9 tests nuevos en `FlujoCajaServiceTest` (bucketing diario/mensual con saldo corrido, exclusión de moneda sin TC con advertencia, cuotas pendientes en sus 3 variantes — ninguna/parcial/todas cobradas —, saldo negativo proyectado, `combinado` con flag `esReal` correcto). Compilado con `mvn clean test-compile`.
- **Frontend**: `tsc -b` y `oxlint` limpios (solo 2 warnings preexistentes ajenos a este paso).
- **E2E real (Docker Compose, MySQL 8)**, con datos y montos conocidos verificados a mano contra la respuesta de la API:
  - Real: cuenta con saldo inicial 10.000 ARS + 2 movimientos bancarios (+5.000, −2.000) → serie diaria exacta (10.000 → 15.000 → 13.000), cuenta en USD sin TC cargado correctamente excluida del consolidado con advertencia (detalle por cuenta en moneda nativa intacto).
  - Proyectado: un Compromiso (3.000) y un Vencimiento (2.000) pendientes aterrizan como egresos exactamente en su fecha; un Compromiso grande (50.000) fuerza saldo negativo, detectado y marcado `saldoNegativo=true` desde ese día en adelante, compuesto correctamente al sumarse el segundo egreso.
  - **CxP pendiente**: una `FacturaCompra` confirmada con `fechaVencimiento` a futuro aparece como egreso (10.890 ARS, saldo con IVA) exactamente en esa fecha, apilándose correctamente sobre los egresos ya proyectados.
  - **Cuota↔cobro**: un `Proyecto` con 2 cuotas (5.000 c/u), una `FacturaVenta` confirmada y un `Cobro` confirmado que la cancela por completo — la cuota 1 (emparejada por orden con el cobro) quedó correctamente excluida de la proyección; la cuota 2 (sin cobro) apareció como ingreso de 5.000 ARS exactamente en su `fechaEstimadaCobro`. El real no se vio afectado por el Cobro, confirmando que no hay doble conteo.
  - `combinado` y ambos exports (Excel/PDF) verificados con HTTP 200 y contenido correcto.
- **UI en navegador** (proxy temporal de CORS, revertido después): la página renderizó el gráfico SVG con el tramo real (línea sólida) y proyectado (línea punteada), los puntos de saldo negativo en rojo, y la tabla de detalle coincidiendo dato a dato con lo verificado por curl.
- **Bug real encontrado y corregido durante la verificación en Docker**: `combinado()` concatenaba las advertencias de `flujoReal()` y `flujoProyectado()` sin deduplicar — la advertencia de "moneda USD sin TC" (que ambas fuentes generan de forma independiente) aparecía duplicada en la UI. Corregido acumulando en un `LinkedHashSet` antes de convertir a lista.

## Bug real encontrado y corregido durante el desarrollo (antes del E2E)

`flujoReal()` construía igualmente una serie consolidada "saldo=0 plano" cuando ninguna cuenta lograba contribuir (todas sin TC cargado) — indistinguible de un consolidado real con saldo cero. Corregido: se rastrea si al menos una cuenta contribuyó (`huboContribucionConsolidado`); si no, el consolidado es una lista vacía en vez de datos fabricados. Encontrado por un test dedicado a ese escenario, no por sospecha previa.

## Notas de infraestructura (no de este paso)

Mismo gap de CORS/proxy documentado desde F2.6 — proxy temporal aplicado solo para verificar, revertido antes del commit.
