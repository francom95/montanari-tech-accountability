# F8.4 — Inversiones (Fondos Fima y similares)

Modelo asignado: Haiku 4.5. Ejecutado con Sonnet 5 (elegido explícitamente por el usuario tras preguntarle, mismo patrón que F4.5/F8.2).

## Qué se construyó

CRUD de **Inversión** (Fondo Fima u otro instrumento, molde PL-1/PL-2) con 1:N **MovimientoInversion** (suscripción/rescate) que genera automáticamente su `MovimientoBancario` en la cuenta de origen. Valuación (cuotapartes × último valor de cuotaparte cargado) y rendimiento calculados on-the-fly, sin columnas persistidas. Vínculo opcional polimórfico a un Compromiso (F8.2) o Vencimiento (F8.1) que, si sigue pendiente, hace aparecer la valuación actual como ingreso proyectado en el flujo de caja (F8.3) exactamente en la fecha de esa obligación — 5ª fuente de la proyección.

## El plan tenía una afirmación falsa — investigado antes de implementar

El plan decía que "el saldo invertido se refleja en el flujo proyectado (F8.3 ya lo consume vía query service: exponerlo)". Se verificó línea por línea `FlujoCajaService.java` (recién cerrado en el paso anterior) y esa afirmación era falsa: no existía ningún hook, campo, ni TODO para inversión — el propio javadoc de la clase decía explícitamente "inversión es F8.4, que depende de este paso". Esto, junto con tres ambigüedades reales más (mecanismo del movimiento en la cuenta de origen, forma del vínculo, fuente del último valor de cuotaparte, moneda de la inversión), se resolvió con el usuario vía `AskUserQuestion` antes de escribir código — cinco decisiones en total, cuatro con la opción recomendada y una explícitamente **no** recomendada:

1. **Mecanismo del movimiento en la cuenta de origen**: `MovimientoBancarioService` (F5.1), no `Cobro`/`Pago` (F4.4) — estos últimos exigen FK a Cliente/Proveedor (modelar un fondo como cliente ficticio sería un hack) y generan asiento automático en cada movimiento, más pesado que la "aritmética simple" que pide el paso.
2. **Forma del vínculo a Compromiso/Vencimiento**: referencia polimórfica (`vinculoTipo`/`vinculoRefId`, sin FK), mismo patrón que `AtribucionImpuesto.liquidacionTipo/liquidacionId` y `Vencimiento.origenGeneracion`.
3. **Fuente del "último valor de cuotaparte cargado"**: el `valorCuotaparte` del movimiento más reciente por fecha — sin entidad nueva de cotizaciones.
4. **Moneda de la Inversión**: heredada de la cuenta de origen — sin campo propio.
5. **Cómo se refleja en el flujo proyectado de F8.3** (la decisión más importante, forzada a preguntar porque la premisa del plan era falsa) — el usuario eligió **"rescate planificado como ingreso proyectado"**, NO la opción recomendada ("solo informativo"): si una Inversión activa tiene vínculo a una obligación pendiente dentro de la ventana proyectada, su valuación actual aparece como ingreso en esa fecha — sin crear ningún "movimiento planificado" en el modelo, se calcula on-the-fly desde la Inversión + su vínculo.

## Decisiones mecánicas (dentro del molde)

- `MovimientoInversion` es un ledger inmutable (sin edición ni eliminación), mismo criterio que `ConsumoTarjeta`.
- Rescate que excede las cuotapartes disponibles → 409 (`CUOTAPARTES_INSUFICIENTES`). Rescate que consume todas las cuotapartes marca automáticamente `estado=RESCATADA_TOTAL`, excluyendo la inversión de proyecciones futuras.
- Conversión de moneda del movimiento bancario generado: ARS usa TC=1; otra moneda reusa el mismo fallback de "última cotización cargada" de `TipoCambioRepository` ya establecido en F8.3 (no es una decisión nueva, es aplicación directa del patrón existente).
- `eliminar` de la Inversión bloqueado con 409 si tiene movimientos asociados (PL-1 estándar).

## Verificación

- **Backend**: 540 tests, 0 fallas propias (Testcontainers ambiental aparte). Tests nuevos: cálculo de valuación/rendimiento (0/parcial/varios movimientos), bloqueo de rescate que excede cuotapartes, marca automática de `RESCATADA_TOTAL`, eliminar bloqueado con movimientos, 3 tests de integración F8.3 (vínculo pendiente dentro de ventana, fuera de ventana, vínculo a id inexistente genera advertencia sin romper). Compilado con `mvn clean test-compile`.
- **Frontend**: `tsc -b` y `oxlint` limpios.
- **E2E real (Docker Compose, MySQL 8)**, migración V38 aplicada limpia sobre las 37 previas:
  - Cuenta con saldo inicial 50.000 ARS + Compromiso futuro (30.000, 2026-08-05) + Inversión vinculada a ese Compromiso.
  - Suscripción de 10.000 ARS (100 cuotapartes a 100) → `MovimientoBancario` generado automáticamente, saldo real de la cuenta cae exactamente a 40.000 ese día (verificado vía `/flujo-caja/real`).
  - Rescate parcial (40 cuotapartes a 102) → cuotapartes acumuladas 60, valuación 6.120, rendimiento 200 — coincide exacto con el cálculo a mano.
  - Intento de rescatar más cuotapartes de las disponibles → 409 `CUOTAPARTES_INSUFICIENTES`, confirmado.
  - Rescate total (60 cuotapartes restantes) → `estado=RESCATADA_TOTAL` automático; confirmado que a partir de ahí la inversión deja de proyectarse en `/flujo-caja/proyectado` (el 05/08 solo queda el egreso de 30.000 del Compromiso).
  - Antes del rescate total: `/flujo-caja/proyectado` mostró exactamente 10.000 de ingreso y 30.000 de egreso el mismo día (05/08) — ambos correctamente separados, sin doble conteo.
- **UI en navegador** (proxy temporal de CORS, revertido después): listado y detalle renderizaron con los datos reales creados por curl — valuación, rendimiento, estado y los 3 movimientos exactos.

## Notas de infraestructura (no de este paso)

Mismo gap de CORS/proxy documentado desde F2.6 — proxy temporal aplicado solo para verificar, revertido antes del commit.
