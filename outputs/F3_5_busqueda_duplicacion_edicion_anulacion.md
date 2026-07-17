# F3.5 — Búsqueda, duplicación, edición y anulación de asientos

**Paso:** 20 de 55 · **Fase:** F3 — Núcleo contable · **Modelo:** Sonnet 5 · **Depende de:** F3.4
**Checkpoint humano:** No.

## Qué se hizo

Sobre el motor manual de F3.4, se completó el ciclo de vida del asiento (F3.1 §4):

- **Búsqueda avanzada:** `AsientoRepository.buscar` ahora filtra por texto (descripción o leyenda de línea), estado, origen, número, rango de fechas, cuenta, importe (debe o haber de cualquier línea), proyecto, cliente y proveedor — los 10 criterios que pedía el paso.
- **Duplicación** (`POST /{id}/duplicar`): copia cualquier asiento (en cualquier estado) a un BORRADOR nuevo — sin número, fecha de hoy, origen `MANUAL` siempre, líneas `generada_auto = false` siempre (F3.1 §4.3).
- **Edición de confirmados** (`PUT /{id}/confirmado`): rebalancea con el mismo checklist de `confirmar` (ahora extraído a `validarChecklistDeAsiento`, compartido entre ambos), sin renumerar ni cambiar de estado. Las líneas `generada_auto = true` solo las puede tocar (modificar o quitar) un ADMINISTRADOR; cualquier usuario puede dejarlas intactas (F3.1 §4.2/§4.6).
- **Anulación por marca** (`PATCH /{id}/anular`): motivo obligatorio, rechaza los orígenes de documento (`ANULACION_VIA_DOCUMENTO`, F3.1 §4.4 D-3) para que asiento y comprobante no diverjan.
- Migración **V19**: columna `motivo_anulacion` en `asiento`.

## Alcance deliberadamente dejado afuera (y por qué)

**El contra-asiento de F3.1 §4.4 para fechas en período cerrado no se implementó.** La decisión de F3.1 es un híbrido: marca si el período está abierto, contra-asiento si está cerrado — pero la entidad `Periodo` todavía no existe (es F9.3). Sin ella no hay forma de determinar "período cerrado", así que **hoy toda anulación es por marca** (el único camino posible mientras ningún período pueda estar cerrado). El admin-override reforzado sobre período cerrado tampoco aplica todavía por la misma razón. Esto es continuación directa de la misma decisión de alcance que F3.4 ya había documentado para el `PeriodoGuard` de confirmación — no es una omisión nueva.

La restricción de "solo ADMIN edita líneas `generada_auto = true`" **sí** se implementó completa y con tests, aunque ningún generador automático (F4.x) existe todavía para producir esas líneas en producción — el campo `generada_auto` ya existe desde F3.4 y la regla es correcta y comprobable hoy mismo (no depende de infraestructura faltante), a diferencia del caso de período cerrado.

## Decisiones tomadas (sin checkpoint, documentadas para trazabilidad)

1. **Comparación de "¿se modificó la línea automática?"** se hace campo a campo (cuenta, debe, haber, moneda, leyenda, dimensiones) contra lo persistido, **excluyendo** `tipoCambio`/`importeOriginal` en líneas ARS: esos dos campos el cliente nunca los reenvía (el service los recalcula siempre al confirmar/editar, F3.1 §3.3), así que no son "contenido editable" a los fines de esta detección.
2. **`motivo_anulacion` es la única columna nueva** para la anulación; quién/cuándo ya salen de `actualizado_en/por` (columnas de auditoría genérica que toda entidad ya tiene) y del `auditoria_log` (acción `ANULAR`) — no se duplicó esa información en columnas propias del asiento, siguiendo el mismo criterio que F3.4 ya había aplicado tácitamente para "confirmado_en/por".
3. **Endpoint separado `PUT /{id}/confirmado`** en vez de hacer que `PUT /{id}` decida solo en base al estado actual: los DTOs son distintos (`AsientoLineaEditarRequest` necesita `id` para poder distinguir líneas existentes de nuevas; el de borrador no lo necesita porque siempre reemplaza todo), y mezclar ambos contratos en un único endpoint hubiera sido más confuso que dos rutas explícitas.

## Verificación realizada

- **170 tests** en la suite completa (156 previos + 14 nuevos de F3.5: duplicar, anular en sus 3 variantes, y 6 variantes de editar confirmado). Solo falla el conocido `ContableApplicationTests` (Testcontainers↔Docker Desktop local), no relacionado.
- **Migración V19 aplicada contra MySQL 8 real** (cadena completa V1→V19) vía `docker compose up`: Flyway migra sin error, Hibernate mapea la entidad con la columna nueva, y —el riesgo real de este paso— **Spring Data JPA valida la nueva consulta JPQL de `buscar`** (con `DISTINCT`, `LEFT JOIN` y 11 parámetros) al arrancar el contexto: si tuviera un error de sintaxis, el `ApplicationContext` no habría levantado. Arrancó limpio, `/actuator/health` → `UP`.
- **Verificación end-to-end contra el servidor real** (vía `curl`, evitando el problema de CORS conocido del dev server): crear → confirmar → editar confirmado (rebalanceo a otro importe, número intacto) → intento de editar a un monto desbalanceado (**422 `ASIENTO_NO_BALANCEA`**, el asiento previo quedó intacto) → duplicar (fecha de hoy, sin número, líneas nuevas) → búsqueda por `numero`, por `cuentaContableId`+`importe` (sin duplicar filas pese al join) y por `fechaDesde` → anular con motivo (**quedó `ANULADO` con `motivoAnulacion` persistido**) → anular sin motivo (**400**, validación).
- **Permisos verificados con un usuario real `LECTURA`** creado en el propio servidor: `duplicar` y `anular` → **403**; listar → **200**. Coincide con la matriz de permisos de F3.1 §4.6.
- Frontend: `tsc -b` limpio; dev server levantado y la app monta sin errores de consola. No se pudo ejercer el flujo completo en navegador (login cruzado con el backend en Docker sigue bloqueado por la falta de CORS, la misma brecha de infraestructura ya documentada en F3.4) — la cobertura funcional real de los nuevos flujos quedó cubierta por la verificación end-to-end vía `curl` contra el servidor real, que ejercita exactamente la misma API que consumen estos hooks.
