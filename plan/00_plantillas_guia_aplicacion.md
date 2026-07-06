# Guía: cómo aplicar las plantillas PL-1..PL-5 a una entidad nueva

> Complementa a [00_plantillas.md](00_plantillas.md) (la definición conceptual). Este archivo es la guía práctica — el molde real vive en el código de `Moneda` (F1.8). **Copiá la forma, no la reinventes.**

## Dónde está el molde real

| Plantilla | Backend/Frontend | Archivos de referencia |
|---|---|---|
| PL-1 (CRUD backend) | `backend/src/main/java/com/montanaritech/contable/maestros/moneda/` | `Moneda.java`, `MonedaRepository.java`, `MonedaService.java`, `MonedaController.java`, `MonedaMapper.java`, `dto/*`, migración `V3__maestros_moneda.sql` |
| PL-1 (tests) | `backend/src/test/java/.../maestros/moneda/` | `MonedaServiceTest.java` (unitario), `MonedaControllerIT.java` (Testcontainers, happy path) |
| PL-2 (CRUD frontend) | `frontend/src/` | `types/moneda.ts`, `hooks/use-monedas.ts`, `pages/monedas-page.tsx` |
| PL-3 (reporte exportable) | ambos | `common/reporte/ReportExportService.java` (motor genérico) + `maestros/moneda/ReporteMonedasController.java` (ejemplo de uso) |
| PL-4 (asiento automático) | backend | `common/asiento/` — `AsientoGenerator.java`, `LineaAsientoGenerada.java`, `AsientoGenerado.java`, `ValidadorBalanceAsiento.java`, `NumeradorAsiento.java` (+ placeholder `NumeradorAsientoEnMemoria`), ejemplo `GeneradorAsientoDePrueba.java` |
| PL-5 (máquina de estados) | backend | `common/estado/EstadoDocumento.java`, `TransicionEstadoValidator.java` |

## PL-1 — CRUD Backend: pasos para una entidad nueva

1. **Migración Flyway**: copiar `V3__maestros_moneda.sql`, cambiar tabla/columnas. Toda entidad de negocio lleva `tenant_id`, `activo`, `creado_en/por`, `actualizado_en/por`, `version` — no son opcionales.
2. **Entity**: `extends EntidadNegocio` (aporta `tenant_id`, timestamps, `version`, filtro de tenant). Solo agregar los campos propios del dominio. Sin lógica de negocio en la entidad — eso vive en el service.
3. **Repository**: `JpaRepository<TuEntidad, Long>` + un método `buscar(...)` con el patrón `(:param IS NULL OR ...)` para filtros opcionales (copiar de `MonedaRepository`).
4. **DTOs**: `XxxResponse` (record, todos los campos de lectura), `XxxCrearRequest`/`XxxEditarRequest` (records con Bean Validation). La clave natural (si existe) va en `Crear` pero NO en `Editar` — no se edita.
5. **Mapper**: interfaz MapStruct de una línea (`aResponse(Entidad): Response`).
6. **Service** — el corazón del molde:
   - `listar`/`obtener`: directos.
   - `crear`: validar unicidad de la clave natural (`ConflictoException` si ya existe) → anotar el método con `@Auditado(accion = CREAR, entidadTipo = "TuEntidad")` (auditoría automática del resultado, no hace falta nada más).
   - `editar`/`activar`/`desactivar`: **sacar el snapshot "antes" (vía el mapper) ANTES de mutar los campos**, mutar, y llamar a `auditoriaService.registrar(ACCION, "TuEntidad", id, antes, después)` a mano. `@Auditado` no sirve acá porque no puede capturar el "antes" (corre después de que el método ya devolvió).
   - `eliminar`: chequear `tieneMovimientosAsociados(id)` antes de borrar. **Este método es el único punto que cambia por entidad**: si tu entidad nueva ya tiene hijos reales (facturas, asientos, etc. la referencian por FK), ese método debe consultar esos repositorios (`facturaRepository.existsByXxxId(id)`) y lanzar `ConflictoException` si hay alguno. Si no tiene hijos todavía, devolver `false` documentando por qué.
7. **Controller**: REST estándar (`GET` lista/detalle abiertos a cualquier rol autenticado; `POST/PUT/PATCH/DELETE` con `@PreAuthorize` según lo que diga el funcional para esa entidad — no asumas los mismos roles que Moneda sin revisarlo).
8. **Tests**: un `XxxServiceTest` (Mockito) cubriendo las validaciones de negocio (duplicado, no encontrado, editar audita, eliminar audita) + un `XxxControllerIT` (Testcontainers) con el ciclo de vida completo contra MySQL real.

## PL-2 — CRUD Frontend: pasos para una entidad nueva

1. **Tipos** (`types/xxx.ts`): espejar los DTOs del backend exactamente (mismos nombres de campo — Jackson serializa camelCase, no hay traducción que hacer).
2. **Hooks** (`hooks/use-xxx.ts`): copiar `use-monedas.ts` completo, cambiar el tipo y las rutas de `http`. Un hook de lista + un hook de mutación por operación (crear/editar/cambiar-estado/eliminar), todos invalidando la misma `queryKey`.
3. **Página** (`pages/xxx-page.tsx`): copiar `monedas-page.tsx`. Cambiar el esquema Zod (debe espejar las validaciones del backend campo por campo), las columnas de la tabla, y los campos del formulario. El patrón de paginación manual (`manualPagination: true`, `pageCount` del `totalPages` de Spring Data) y el manejo de 409 (capturar en `onError`, leer `error.response.data.detail`) se copian igual.
4. **Ruta y nav**: agregar la entrada en `routes/nav-config.ts` y el `path` en `routes/router.tsx`.

## PL-3 — Reporte exportable

No reimplementar POI/OpenPDF. `ReportExportService` (ya escrito, genérico) recibe título + columnas + filas y escribe Excel o PDF. Un reporte nuevo solo necesita:
1. Un controller con `GET` (JSON, con los filtros estándar que apliquen: período, proyecto, cuenta, cliente, proveedor, moneda) + `GET /exportar/excel` + `GET /exportar/pdf` que arman las filas y llaman a `reportExportService.exportarExcel(...)`/`exportarPdf(...)` dentro de un `StreamingResponseBody` (copiar `ReporteMonedasController` literal).

## PL-4 — Asiento automático

Cuando F4+ implemente un generador real (factura de venta, cobro, etc.):
1. `implements AsientoGenerator<TuEvento>`.
2. Armar las líneas desde la tabla de mapeo concepto→cuenta (editable por admin) — nunca hardcodear cuentas como hace `GeneradorAsientoDePrueba` (ese es solo de demostración).
3. Llamar a `ValidadorBalanceAsiento.validar(lineas)` antes de devolver nada.
4. Pedir el número una sola vez a `NumeradorAsiento` (y reemplazar `NumeradorAsientoEnMemoria` por la implementación real respaldada en base — F3.1 — antes de usar esto en producción).
5. Completar `documentoOrigenTipo`/`documentoOrigenId`.

## PL-5 — Máquina de estados

Se aplica a facturas, cobros, pagos y asientos (no a Moneda — no tiene sentido, Moneda no es un documento transaccional). Para una entidad con estados:
1. La entidad tiene una columna `estado` (`EstadoDocumento` enum).
2. El service llama a `TransicionEstadoValidator.validar(actual, nuevo)` antes de aplicar el cambio.
3. Auditar con `auditoriaService.registrar(CAMBIO_ESTADO, ...)` igual que `MonedaService.activar/desactivar`.
4. Solo `CONFIRMADO` impacta contabilidad/reportes; `ANULADO` nunca borra el registro.

## Errores comunes a evitar (ya los pisamos una vez en F1.5/F1.6)

- No dejar que una excepción de Spring Security (`AuthorizationDeniedException`, `AuthenticationException`) caiga en el `@ExceptionHandler(Exception.class)` genérico — ya están cubiertas en `GlobalExceptionHandler`, no hace falta tocarlo por cada entidad nueva.
- `@Auditado` solo sirve para "crear" (no hay "antes"). Para editar/eliminar/cambiar estado, auditoría explícita de una línea.
