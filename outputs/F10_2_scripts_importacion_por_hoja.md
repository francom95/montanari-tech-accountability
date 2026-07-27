# F10.2 — Scripts de importación por hoja

Modelo asignado: Haiku 4.5. Usado: Sonnet 5 (mismo precedente que F4.5/F9.1/F9.2 — el usuario eligió seguir con Sonnet 5 al preguntarle por la discrepancia).

## Qué se construyó

8 importadores backend (`previsualizar`/`confirmar`), uno por hoja/entidad del mapeo de F10.1, replicando el molde de `EtapaImportService` (F2.5): Cliente, Proveedor, Proyecto+ProyectoCuota, ComisionProyecto, Compromiso, Inversion+MovimientoInversion, PendienteAdministrativo y Asiento+AsientoLinea (el núcleo — Libro Diario completo). Sin UI (decisión del usuario, carga histórica de una sola vez). Nueva utilidad compartida `common/importacion/ImportUtils` (extraída de la lógica antes duplicada en `EtapaImportService`, que queda intacto).

## Decisiones pre-resueltas con el usuario (antes de codear)

1. **`ProyectoCuota` sin fecha** → relajado (`@NotNull` removido de `CuotaRequest.fechaEstimadaCobro`, migración V44 hace la columna nullable). `FlujoCajaService.cuotasPendientes` ajustado para saltear cuotas sin fecha (evita NPE real, encontrado por inspección antes de que la E2E lo disparara).
2. **Comisiones sin `%`** (12/14 filas) → `%` sintético (`monto ÷ montoTotal × 100`) + `editar()` inmediato para fijar `importeFinal` al monto real de la hoja.
3. **Sin UI** → confirmado, solo endpoints backend.
4. **CUIT faltante en Cliente/Proveedor** → **no se relaja la validación** (a diferencia del punto 1): filas sin CUIT se rechazan, no se migran. Decisión explícita del usuario, no la recomendada por el asistente.

## Alcance

Importadores de código (8, listados arriba). Fuera de alcance (ya resuelto en F10.1, sin código nuevo): 5 `Concepto`s recurrentes de `Presupuesto de Pagos` §2-6 (alta manual vía CRUD existente), `CALENDARIO DE VENCIMIENTOS` (hoja vacía), `Comisionista` maestro (alta manual, el importador de ComisionProyecto rechaza si no existe), `PresupuestoLineaCosto` (no hay `Proveedor` migrado contra el cual engancharlo, dado el punto 4).

## Resultados reales de la migración (E2E, Docker Compose + MySQL 8, Excel real `Contabilidad 2026 (3).xlsx`)

| Importador | Filas/grupos | Creadas | Rechazadas | Motivo del rechazo |
|---|---|---|---|---|
| Cliente | 15 | **0** | 15 | CUIT no informado (decisión 4) — peor que la estimación de F10.1 (2/15); con los datos reales, ningún cliente trae CUIT |
| Proveedor | 9 | **0** | 9 | CUIT no informado (decisión 4) |
| Proyecto | 31 | **0** | 31 | Cascada: `Cliente no encontrado` (consecuencia directa de 0 clientes migrados) |
| ComisionProyecto | 15 | **0** | 15 | Cascada: `Proyecto no encontrado` |
| Compromiso | 13 | **13** | 0 | — |
| Inversion (`MovimientoInversion`) | 6 filas (3 filtradas) | **3** | 0 | 3 filas de "Valuación del Fondo Fima" (Operación vacía) descartadas silenciosamente por diseño (no son error, son ruido esperado de la hoja) |
| PendienteAdministrativo | 16 (10+6, dos archivos) | **16** | 0 | — |
| Asiento (Libro Diario, dos archivos) | 170 grupos (27+143) | **159** | 11 | Ver detalle abajo |

**Cliente/Proveedor/Proyecto/ComisionProyecto en 0 no es un bug**: es la decisión 4 (no relajar CUIT) propagándose en cascada exactamente como se documentó en el plan aprobado. Verificado contra la base real: el único Cliente/Proveedor/Proyecto que existen en el sistema (`id=1` en cada tabla) son fixtures de prueba de F9.2 (`Buscatest ... SA`), no datos de esta importación.

**Asiento — detalle de los 11 grupos genuinamente rechazados** (datos de origen incompletos/desbalanceados en el Excel, no un bug del importador):
- Archivo 1 (sep25-abr26): grupo 4 ("Reconocimiento de IIBB - Agosto", sin debe/haber), grupo 10 y grupo 11 (una sola línea cada uno, no alcanza para balancear un asiento de partida doble).
- Archivo 2 (Libro Diario): grupos 45 y 58-61 y 79-80 (una sola línea), grupo 46 (debe=0, haber=144.676,68 — desbalanceado).

Total real migrado: **159 Asientos CONFIRMADOS** + 13 Compromisos + 16 Pendientes + 1 Inversion (3 movimientos) = base contable histórica cargada. Verificado además que correr `confirmar` una segunda vez sobre las mismas filas no duplica nada (cae en `yaExistian`).

## Dos bugs reales encontrados y corregidos durante la E2E

1. **`auditoria_log.entidad_id NOT NULL` vs. log de resumen por lote**: los 8 importadores llamaban `auditoria.registrar(..., null, ...)` para un log de resumen, violando la constraint en el primer `confirmar()` real (HTTP 500 en Cliente). Cada entidad ya tiene su propio log de auditoría individual con `entidadId` real (vía el `crear()` de cada servicio de dominio), así que el resumen por lote se eliminó en vez de inventar un id sentinel. Corregido en los 8 `*ImportService` + sus tests.
2. **Idempotencia vs. estado de confirmación en Asiento**: `existsByFechaAndDescripcion` solo chequeaba existencia, no estado. Cuando `crearBorrador()` tenía éxito pero el `confirmar()` posterior fallaba (validación de negocio: <2 líneas o desbalance), el `BORRADOR` huérfano satisfacía el chequeo de existencia en cualquier reintento posterior — ese grupo quedaba contado como "ya existía" para siempre, nunca reintentado ni reportado como rechazado, sin forma de detectarlo salvo cruzando el trail de auditoría a mano. Encontrado comparando conteos esperados de grupos contra los reales. Corregido: `existsByFechaAndDescripcionAndEstado(fecha, descripcion, CONFIRMADO)` — ahora el reintento vuelve a intentar el grupo y, si vuelve a fallar, lo reporta con su motivo real.

## Decisión de diseño: `AsientoImportService.confirmar()` sin `@Transactional`

A diferencia de los otros 7 importadores, deliberadamente **sin** `@Transactional` a nivel de método: `AsientoService.crearBorrador`/`confirmar` ya son transaccionales de forma independiente (propagación REQUIRED). Envolver el método del importador en una única transacción haría que la excepción de negocio de un grupo (ej. desbalance) marque esa transacción compartida como rollback-only — y aunque el `catch` la atrape y siga procesando, **todos** los asientos ya confirmados en la misma corrida se perderían al hacer rollback al final. Sin el wrapper, cada grupo commitea independientemente.

## Nota operativa: BORRADOR huérfanos en la base real

La base de desarrollo tiene actualmente **22 filas `Asiento` en estado `BORRADOR`** (para los 10 números de grupo genuinamente rotos que sí llegaron a `crearBorrador`: 10, 11, 45, 46, 58, 59, 60, 61, 79, 80 — con más de una fila por número porque se reintentó `confirmar` varias veces durante esta sesión de verificación, y cada intento crea un nuevo `BORRADOR` antes de fallar en el `confirmar`). Comportamiento esperado y documentado en el propio mensaje de rechazo ("revisar si quedó un BORRADOR huérfano para eliminar a mano") — se dejan para revisión manual del equipo, no se borran automáticamente. No afectan reportes/saldos (un `BORRADOR` no impacta el Mayor).

## Verificación

- **Backend**: 649/650 tests (la única falla es el error ambiental de Testcontainers ya documentado en memoria, no relacionado a este paso). ~56 tests nuevos entre los 8 importadores.
- **E2E real** (Docker Compose + MySQL 8, volumen reusado con datos previos de F9.x, Excel real `Contabilidad 2026 (3).xlsx` extraído hoja por hoja con un script Python de scratchpad): los 8 importadores corridos en el orden de dependencias de F10.1, contra datos reales, con conteos reconciliados a mano contra la base (ver tabla arriba). Confirmado que una segunda corrida de `confirmar` sobre las mismas filas no duplica nada.
- Sin verificación de UI (no hay frontend en este paso, por decisión explícita del usuario).
