# F8.2 — Presupuesto de pagos

Modelo asignado: Haiku 4.5. Ejecutado con Sonnet 5 (elegido explícitamente por el usuario tras preguntarle, siguiendo el patrón ya visto en F4.5).

## Qué se construyó

CRUD de **Compromiso** (egreso presupuestado a nivel plan, previo a convertirse en un `Vencimiento` concreto de calendario), molde PL-1 (backend) + PL-2 (frontend) replicado mecánicamente de F1.8/Moneda, con un agregado puntual: al crear un compromiso, un checkbox opcional genera además su vencimiento en F8.1, reusando el mismo mecanismo de trazabilidad (`origenGeneracion`/`origenGeneracionRefId`) ya construido para el motor de generación automática.

## Por qué no es un duplicado de Vencimiento (F8.1)

Investigado antes de implementar: ni `plan/41_...` (F8.1) ni `plan/42_...` (F8.2) explicitan en prosa la diferencia, pero `plan/43_F8_3_...` (flujo de caja proyectado) y `plan/44_F8_4_...` (inversiones) sí las tratan como **dos fuentes paralelas e independientes** que alimentan la misma proyección — nunca una reemplaza a la otra. `Compromiso` es el ítem de planificación (sin cuenta contable/tarjeta/liquidación asociada, solo proveedor+proyecto opcionales); `Vencimiento` es la entrada concreta de calendario con fecha cierta. El vínculo es unidireccional y opcional: un compromiso *puede* generar su vencimiento, nunca al revés.

## Decisiones mecánicas (no de diseño, dentro del molde)

- `TipoCompromiso` (13 valores del plan) → `TipoVencimiento` (13 valores de F8.1, distintos): mapeo explícito en `CompromisoService` vía `Map<TipoCompromiso, TipoVencimiento>`; los tipos de Compromiso sin equivalente directo en Vencimiento (pago a proveedor, comisión bancaria, comisión por venta, vencimiento impositivo genérico) caen a `OTRO`.
- `VencimientoService.crear()` se refactorizó a `crearDesdeOrigen(req, origen, origenRefId)` (con `crear()` delegando a `MANUAL`/`null`) para que `CompromisoService` pueda crear el vencimiento con `origenGeneracion=COMPROMISO` sin duplicar lógica de resolución de FKs.
- "Eliminar solo si no hay movimientos asociados" (PL-1): el "movimiento asociado" de un Compromiso es su propio vencimiento generado — se verifica con el campo `vencimientoGeneradoId` ya guardado en la entidad, sin query adicional. Bloqueo con `ConflictoException` → HTTP 409.
- `estado` (PENDIENTE/RESUELTO/CANCELADO) es un campo de negocio explícito del plan, separado de `activo` (soft-delete estándar de PL-1) — no tiene ciclo de vida propio con endpoints dedicados, se edita como cualquier otro campo vía `PUT`.

## Verificación

- **Backend**: 513 tests, 0 fallas propias (Testcontainers ambiental aparte). 8 tests nuevos en `CompromisoServiceTest` (CRUD, generación de vencimiento vinculado con mapeo de tipo correcto, eliminar bloqueado con vencimiento generado, eliminar exitoso sin vencimiento, query por rango de fechas). Compilado con `mvn clean test-compile` (no solo `compile`) desde el arranque, por la lección de F8.1.
- **Frontend**: `tsc -b` y `oxlint` limpios.
- **E2E real (Docker Compose, MySQL 8)**: migración V37 aplicada limpia sobre las 37 previas. Creado un compromiso sin generar vencimiento y otro con el checkbox activo — el segundo generó correctamente su `Vencimiento` (`origenGeneracion=COMPROMISO`, tipo mapeado `CUOTA_PLAN_DE_PAGOS→PLAN_DE_PAGO`). Confirmado el bloqueo 409 al intentar eliminar el compromiso con vencimiento generado, y el éxito (204) al eliminar el que no tenía. Confirmado el query `/por-rango-de-fechas` devolviendo exactamente el compromiso esperado.
- **UI en navegador** (con el mismo proxy temporal de CORS de F8.1, revertido después): formulario y listado renderizan correctamente con datos reales, incluida la columna "Vencimiento" mostrando "Generado (#1)"; se creó un compromiso nuevo interactivamente desde la UI y apareció en el listado sin generar vencimiento (checkbox no marcado), confirmando el flujo de escritura completo.

## Notas de infraestructura (no de este paso)

Mismo gap de CORS/proxy documentado desde F2.6 — proxy temporal aplicado solo para verificar, revertido antes del commit.
