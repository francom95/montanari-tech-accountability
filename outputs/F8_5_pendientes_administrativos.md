# F8.5 — Pendientes administrativos

Modelo asignado: Haiku 4.5. Ejecutado con Sonnet 5 (elegido explícitamente por el usuario tras preguntarle, mismo patrón que F4.5/F8.2/F8.4).

## Qué se construyó

CRUD de **PendienteAdministrativo** (molde PL-1/PL-2 puro, sin gaps de diseño reales — investigado antes de implementar y confirmado): recordatorios, controles manuales, revisiones del contador, ajustes pendientes, facturas a pedir, pagos a verificar, movimientos bancarios a identificar, impuestos a revisar. Campos: título, descripción, fecha estimada de resolución, prioridad (alta/media/baja), estado (pendiente/en proceso/resuelto/cancelado/postergado), responsable (FK Usuario opcional), categoría (texto libre), proyecto/cliente/proveedor (FK opcionales), observaciones. Query service `proximosAVencer(dias)` para que F9.1 (alertas) consuma los pendientes por vencer, mismo molde que `VencimientoService.proximos`. Frontend: listado con filtros por estado/prioridad/responsable/categoría, reemplazando el placeholder de `/pendientes` que ya existía desde el scaffolding inicial.

## Investigación previa (sin gaps reales encontrados)

A diferencia de F8.4, esta investigación **no** encontró ninguna afirmación falsa ni ambigüedad real en el plan — se confirmaron los tres puntos de mayor riesgo antes de escribir código:

1. **Responsable (Usuario)**: `Proyecto.responsable` (F2.5) ya establece el patrón exacto — FK opcional a `com.montanaritech.contable.auth.Usuario`, resuelto vía `resolverResponsable(id)` con `RecursoNoEncontradoException` si no existe. Replicado sin cambios.
2. **Categoría**: se verificó que `Categoria` (maestros.categoria) es específicamente la categoría contable del plan de cuentas (ACTIVO/PASIVO/PN/RP/RN/OTROS_RESULTADOS) — reutilizarla para "categoría" de un pendiente administrativo hubiera sido semánticamente incorrecto. Se usa un campo `String categoria` libre, sin FK.
3. **"Fecha de creación (auto)"**: ya la aporta `creadoEn` de la auditoría estándar (`EntidadNegocio`) — no se agregó un campo de negocio duplicado.

## Decisiones mecánicas (dentro del molde)

- `eliminar` es un delete simple, sin condición de bloqueo — a diferencia de `Compromiso` (bloqueado si generó un Vencimiento) o `Inversion` (bloqueada si tiene movimientos), un `PendienteAdministrativo` no tiene entidades hijas dependientes.
- La lista se ordena por `fechaEstimadaResolucion ASC, id DESC` (los sin fecha quedan primero por el orden NULL-first de MySQL en ASC) — se descartó `NULLS LAST` de HQL por falta de precedente probado en el resto del código, priorizando consistencia sobre una sintaxis no verificada en este entorno.
- Kanban por estado (mencionado como opcional en el plan) se omitió: no hay precedente de drag-and-drop en el resto del proyecto y el plan explícitamente permite que la lista alcance si el tablero no es directo de implementar.

## Verificación

- **Backend**: 547 tests, 0 fallas propias (Testcontainers ambiental aparte). Tests nuevos: CRUD con FKs opcionales null y resueltas, responsable inexistente lanza 404, editar actualiza estado y audita, eliminar funciona, `proximosAVencer` delega correctamente en el repositorio con `estado=PENDIENTE`. Compilado con `mvn clean test-compile`.
- **Frontend**: `tsc -b` y `oxlint` limpios.
- **E2E real (Docker Compose, MySQL 8)**, migración V39 aplicada limpia sobre las 38 previas: creados 3 pendientes (uno con responsable/categoría/fecha próxima, uno sin opcionales, uno sin fecha estimada). Verificados exactos: filtros por `estado`, `prioridad`, `responsableId` y `categoria`; `proximos?dias=10` incluyó correctamente el pendiente con fecha dentro de la ventana y `proximos?dias=3` lo excluyó; al editar el pendiente a `estado=RESUELTO`, `proximosAVencer` dejó de incluirlo (confirma el filtro `estado=PENDIENTE` del query service); `desactivar` + filtro `activo=false` correctos; `eliminar` devolvió 204.
- **UI en navegador** (proxy temporal de CORS, revertido después): el listado renderizó exactamente los pendientes reales creados por curl, con las etiquetas de estado/prioridad correctas y los botones Activar/Desactivar reflejando el campo `activo` real de cada uno.

## Notas de infraestructura (no de este paso)

Mismo gap de CORS/proxy documentado desde F2.6 — proxy temporal aplicado solo para verificar, revertido antes del commit. Durante la verificación de UI, los clics simulados por coordenadas (`computer left_click`) no dispararon el submit del formulario de login en este entorno (el panel de navegador no estaba componiendo frames visualmente); se resolvió disparando el evento nativamente vía `dispatchEvent`/`.click()` por JavaScript — no es un bug de la aplicación, confirmado porque el mismo flujo de login funcionó normalmente en F8.1–F8.4 con la misma técnica de clic.
