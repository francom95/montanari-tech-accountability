# F9.2 — Búsqueda global "Lupita"

Modelo asignado: Sonnet 5 (sin discrepancia con la sesión activa).

## Qué se construyó

Barra de búsqueda unificada (Ctrl/Cmd+K + ícono en el header) sobre 16 tipos de entidad. Detecta el tipo de término (CUIT/importe/fecha/texto) una única vez y federa la búsqueda contra el repositorio apropiado de cada tipo, agrupando los resultados por entidad con un límite de 5 por grupo + total real. Primer uso de `FULLTEXT` de MySQL en el proyecto, restringido a las columnas de texto libre que antes solo soportaban `LIKE` (`asiento.descripcion`, `movimiento_bancario.descripcion`, `vencimiento.descripcion`, `pendiente_administrativo.titulo`, y los `nombre` de `cliente/proveedor/proyecto/etapa/cuenta_contable`).

## Decisión de diseño (1 gap real, resuelto con la opción NO recomendada)

Solo 4 de las 16 entidades tienen una ruta de detalle propia (`Proyecto`, `TarjetaCredito`, `Inversion`, Mayor de `CuentaContable`); las otras 12 son pantallas de lista con edición inline. En vez de navegar a la lista con el texto precargado (recomendado, pero ambiguo si hay más de un resultado similar), el usuario eligió **agregar un filtro exacto por `?id=`** a esas 12 pantallas: cada una, al detectar el parámetro, hace un fetch puntual por id (nuevo hook `useX(id)` por entidad) y renderiza esa única fila en la misma tabla, ocultando filtros/paginación y mostrando un link "Ver todos".

## Decisiones mecánicas dentro del molde

- **Tenant explícito en SQL nativo**: `@Filter` de Hibernate no aplica a `nativeQuery = true` — cada `MATCH(...) AGAINST(...)` (asiento, movimiento_bancario) lleva `WHERE tenant_id = :tenantId` explícito, resuelto desde `TenantContext.getTenantId()`. Verificado en E2E con 2 tenants reales (no solo con mocks): un mismo término compartido entre un movimiento bancario de cada tenant devolvió únicamente el propio en cada búsqueda.
- **Reuso del molde `buscar(texto)` existente**: donde ya existía una query `LIKE` tenant-safe (Cliente, Proveedor, Proyecto, CuentaContable, TarjetaCredito, FacturaVenta/Compra, PendienteAdministrativo), se reusó directamente para el término TEXTO en vez de duplicar con FULLTEXT — el índice FULLTEXT se agregó a esas columnas por si en el futuro se necesita relevancia real, pero hoy no se consulta vía `MATCH` en esas entidades.
- **`DetectorTerminoBusqueda`**: utilidad estática, cascada CUIT → FECHA → IMPORTE → TEXTO (primer match gana). Tolerancia de importe fija ±1% con piso de $1.00 (evita división por cero en montos chicos) — verificado en E2E: un importe real de $123.456,78 matcheó con un término de búsqueda de $123.400 (dentro de tolerancia) pero no con $100.000 (fuera).
- **`ETAPA` sin ruta propia**: vive en un tab de `ProyectoDetallePage`. El `ResultadoItem` de tipo `ETAPA` lleva un `contextoId` (el id del proyecto) para que el frontend arme `/proyectos/{contextoId}?tab=etapas&id={etapaId}` — requirió que `ProyectoDetallePage` lea `?tab=` en su estado inicial (antes hardcodeado a `"datos"`) y pase `etapaResaltadaId` a `EtapasTab` para hacer scroll + resaltar la fila.
- **Filtro `?id=` en las 12 páginas de lista**: mismo patrón replicado sin variación — `useSearchParams()` lee `id`, un hook `useX(id)` nuevo hace el fetch puntual (`enabled: id !== undefined`), la tabla usa esa única fila en vez de la página paginada, y se oculta la barra de filtros/paginación detrás de `idFiltro === undefined`. Dos variantes del molde por estructura de página preexistente: `movimientos-bancarios-page.tsx` no tenía `useReactTable` (solo un `.map()` plano) y `vencimientos-page.tsx`/`liquidacion-iva-page.tsx`/`liquidacion-iibb-page.tsx` ya tenían un patrón de detalle por selección (`seleccionado`/`seleccionadaId` + `.find()`) que se adaptó para usar el fetch directo por id en vez de buscar dentro de la página cargada actualmente (evita el caso de que el registro buscado esté fuera del año/filtro seleccionado).
- **Frontend sin librería nueva**: mismo criterio que F9.1 — no existe ningún primitive de Dialog/Modal/Popover en el proyecto, así que el overlay de búsqueda es un `div` centrado posicionado a mano, con Escape/click-afuera para cerrar y debounce de 300ms.

## Verificación

- **Backend**: 572 tests, 0 fallas propias (Testcontainers ambiental aparte, documentado en memoria del proyecto). Tests nuevos: `DetectorTerminoBusquedaTest` (9 casos puros de detección), `BusquedaGlobalServiceTest` (7 casos Mockito: dispatch por cada tipo de término, aislamiento de tenant con 2 valores de `TenantContext` sobre repos mockeados, límite por grupo vs. total real). Compilado con `mvn -o clean test-compile` + `mvn -o test`.
- **Frontend**: `tsc -b` y `oxlint` limpios en las 12 páginas + overlay + hooks nuevos (únicos 2 warnings son pre-existentes en `components/ui/*`, no de este paso).
- **E2E real (Docker Compose, MySQL 8)**, migración V42 (FULLTEXT) aplicada limpia sobre las 41 previas:
  - CUIT: cliente con `20-12345678-6` encontrado por CUIT exacto y por texto libre del nombre.
  - Texto ambiguo: "Buscatest" matcheó simultáneamente `CLIENTE` y `PROVEEDOR` en la misma respuesta (2 grupos).
  - Fecha: `15/08/2026` encontró el `VENCIMIENTO` con esa fecha exacta.
  - Importe con tolerancia: `123400` matcheó un vencimiento de `$123.456,78` (dentro de ±1%); `100000` no matcheó nada (fuera de tolerancia).
  - **Aislamiento de tenant en SQL nativo** (el riesgo real que los tests unitarios con mocks no cubren): creado un tenant 2 completo (moneda/cuenta contable/cuenta bancaria propias) y un movimiento bancario en cada tenant con la misma descripción única (`AislamientoLupitaUnico`) — la búsqueda en tenant 1 devolvió únicamente su propio movimiento, la de tenant 2 únicamente el suyo.
  - Click-through: resultado de `CLIENTE` (una de las 12 páginas `?id=`) navegó a `/clientes?id=1` y la tabla mostró únicamente esa fila con el link "Ver todos". Resultado de `ETAPA` navegó a `/proyectos/1?tab=etapas&id=1`, aterrizando directo en el tab "Etapas" con la etapa visible.
- **UI en navegador** (proxy temporal de CORS, revertido después — ver nota de infraestructura): login, ícono de lupa en el header abre el overlay, debounce funcionando, resultados agrupados por tipo con headers, clicks navegando y cerrando el overlay.

## Notas de infraestructura (no de este paso)

Mismo gap de CORS/proxy documentado desde F2.6 — proxy temporal aplicado solo para verificar (`vite.config.ts` + `.env` con `VITE_API_BASE_URL` relativo), revertido antes del commit (`git diff` confirmó reversión limpia en `vite.config.ts`; `.env` no está versionado). Durante la verificación de UI, el panel de navegador nuevamente no compuso frames visualmente (mismo síntoma que F8.5/F9.1) — se usó el mismo workaround ya documentado (`dispatchEvent`/`.click()` vía JavaScript) para login, apertura del overlay y clicks de resultados, con éxito. Además se descubrió que el puerto 8080 tenía un proceso stale de una sesión anterior ocupando el puerto (el backend real de Docker Compose expone 8081→8080) — causó confusión inicial de "credenciales inválidas" que en realidad era un 401 de un servicio completamente distinto.
