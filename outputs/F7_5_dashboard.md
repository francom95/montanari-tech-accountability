# F7.5 — Dashboard

**Paso:** 39 de 55 · **Fase:** F7 — Reportes y dashboard · **Modelo asignado:** Sonnet 5 · **Depende de:** F7.4
**Checkpoint humano:** No.

## Qué se hizo

La pantalla de inicio del sistema: 12 indicadores del período seleccionado (ventas, cobros, cuentas por cobrar/pagar, obligaciones próximas, saldo de caja, saldo por banco/cuenta, margen estimado, egresos proyectados, impuestos próximos a vencer de IVA e IIBB, y zona de alertas), cada uno con drill-down a su reporte de origen, servidos por un único endpoint agregado y cacheado 2 minutos. El plan pedía "no rediseñar lo ya decidido", pero surgieron 4 puntos reales sin resolver — todos zanjados por el usuario en esta sesión, con más alcance del mínimo elegido a propósito.

### Saldo real de caja/banco (adelanta parte de F8.3)

`RecalculoSaldoService` era un passthrough para `CuentaBancaria` (devolvía el saldo inicial sin tocar). La lógica real ya existía, duplicada como método privado en `ConciliacionService` (F5.3), nunca conectada a `saldoActual`. Se extrajo a `RecalculoSaldoService.recalcularCuentaBancariaHasta(cuenta, fechaHasta)` (mismo lugar que la de `TarjetaCredito`, F5.4) y `ConciliacionService` pasa a reusarla — cero cambio de comportamiento en F5.3, solo se eliminó la duplicación. Semántica: saldo inicial + Σ `MovimientoBancario` no `DESCARTADO` (`PENDIENTE` y `CONCILIADO` cuentan igual) desde `fechaSaldoInicial` hasta la fecha pedida, ambos extremos inclusive.

### Impuestos próximos a vencer

No hay ningún campo de fecha de vencimiento en el dominio impositivo. Se agregó `ConfiguracionDashboard` (mismo patrón singleton que `ConfiguracionAtribucion`/`ConfiguracionPresupuesto`, migración V35) con el día fijo del mes siguiente en que vence IVA e IIBB, editable por admin.

### Margen estimado

No existía ningún agregado entre proyectos (el presupuesto de F2.6 es siempre en USD, por proyecto). Se reusa el emparejamiento cuota↔factura de F7.4 **sin tocar su código**: por cada proyecto activo con presupuesto y al menos un pago emparejado, `ReporteRentabilidadProyectoService.obtener(id)` ya devuelve `presupuestoConvertidoArs` y `precioFinalCliente`/`cantidadPagosPactados`; se deriva el TC efectivo de la porción emparejada (`presupuestoConvertidoArs ÷ (precioFinalCliente/cantidadPagosPactados × pagosEmparejados)`) y se aplica a `margenDeseadoUsd`.

### Cacheo

No había infraestructura de caché en el proyecto. Se agregó `spring-boot-starter-cache` + Caffeine, TTL de 2 minutos, `@Cacheable("dashboard")` en el único endpoint agregado; guardar la configuración limpia el caché para reflejar el cambio sin esperar el TTL.

### Bug real encontrado durante la verificación E2E

El saldo de caja/banco sumaba `CuentaBancaria.saldoActual` de **todas** las cuentas activas sin importar su moneda — mezclando ARS y USD en una sola cifra rotulada "ARS", algo que ningún reporte anterior hacía (F5.3 siempre opera sobre una cuenta a la vez). Corregido con el mismo criterio que F7.4 usa para comisiones en otra moneda: se excluyen las cuentas no-ARS (sin asumir un TC) y se avisa la exclusión en `alertas` — el mismo slot que el plan dejaba listo para F9.1.

### Alcance del resto de indicadores (documentado, sin pregunta adicional)

"Obligaciones próximas" = facturas de compra `POR_VENCER` dentro de la ventana configurable (distinto de "cuentas por pagar", que no tiene ventana). "Egresos proyectados" = Σ `pagosPrevistos + costosEstimados` de etapas no canceladas de proyectos activos (foto del portafolio, no acotada al período). "Ventas"/"Cobros" del período son sumas nuevas sobre `FacturaVenta`/`Cobro` confirmados (ventas neta de notas de crédito, mismo criterio de signo que F6.3).

## Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **482/483 en verde** (único fallo esperado: Testcontainers/Docker Desktop local). Nuevos: `DashboardServiceTest` (10 casos, uno por indicador + margen estimado con 2 escenarios + exclusión de moneda), extensión de `RecalculoSaldoServiceTest` (4 casos nuevos de `CuentaBancaria`).
- Frontend: `tsc -b` y `oxlint` limpios (0 warnings nuevos).

### E2E contra MySQL 8 real (docker-compose)

Escenario: 2 cuentas bancarias ARS (Caja Chica $10.000 sin movimientos, Banco Galicia CC con 3 movimientos: uno imputado→CONCILIADO $200.000, uno PENDIENTE $50.000, uno DESCARTADO $999.999) + 1 cuenta USD (para probar la exclusión) + 1 liquidación de IVA de julio en BORRADOR con saldo a pagar $63.000.

| Verificación | Resultado | ✔ |
|---|---|---|
| Saldo de caja | $10.000,00 (sin movimientos) | ✔ |
| Saldo de banco | $250.000,00 = $200.000 (conciliado) + $50.000 (pendiente); el descartado ($999.999) no suma | ✔ |
| Vencimiento IVA | 2026-08-20 (día 20 del mes siguiente), saldo $63.000,00 = saldo a pagar real de la liquidación | ✔ |
| Vencimiento IIBB | 2026-08-15, saldo $0,00 (sin liquidación cargada, sin crashear) | ✔ |
| Alertas | "1 cuenta(s) bancaria(s) en otra moneda excluida(s)..." | ✔ |
| PUT configuración → GET dashboard | Cambiar día de IVA de 20 a 10 se refleja de inmediato (caché invalidada, sin esperar el TTL) | ✔ |
| Indicadores sin datos (ventas, cobros, CxC/CxP, margen, egresos) | $0,00, sin error | ✔ |
| Frontend en vivo (vite dev + proxy temporal a la API dockerizada) | Login, sidebar, 10 tarjetas + impuestos + alertas + card de configuración, todos los valores coinciden exactamente con la API | ✔ |
| Migración V35 | Aplicada, tabla `configuracion_dashboard` sembrada | ✔ |

Nota de infraestructura (no de este paso): el login desde el frontend dockerizado (puerto 5173, origen distinto al backend) recibe 403 en el preflight OPTIONS — el backend no tiene CORS configurado, brecha ya documentada desde F2.6/memoria. Se verificó el frontend real con el dev server de Vite + un proxy temporal a la API dockerizada (mismo origen, sin CORS), revertido después de verificar.
