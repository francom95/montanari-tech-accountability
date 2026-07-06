# Montanari Tech — Sistema de Gestión Contable

[![CI](https://github.com/montanaritech/montanari-accounting/actions/workflows/ci.yml/badge.svg)](https://github.com/montanaritech/montanari-accounting/actions/workflows/ci.yml)

Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech. Ver [CLAUDE.md](CLAUDE.md) y `./plan` para el proceso de desarrollo orquestado; este README es sobre el **sistema en sí** (`./backend`, `./frontend`).

## Estructura

```
backend/    Java 21 + Spring Boot 3.x (Maven)
frontend/   React 18+ + TypeScript + Vite, shadcn/ui + Tailwind (F1.2/F1.4)
docker-compose.yml
.env.example
```

## Arranque rápido (Docker)

```bash
cp .env.example .env
docker compose up --build
```

- Backend: http://localhost:8081 (puerto host no estándar para no chocar con otra instancia local; internamente el contenedor sigue en 8080)
  - Health: http://localhost:8081/actuator/health
  - Swagger UI: http://localhost:8081/swagger-ui.html
- Frontend: http://localhost:5173
- MySQL expuesto en `localhost:3307` (puerto host no estándar para no chocar con otra instancia local; usuario/clave de `.env`)

Flyway corre las migraciones automáticamente al arrancar el backend, incluyendo el usuario admin inicial (`admin@montanaritech.com` / `changeme123` — rotar antes de cualquier uso real).

## Desarrollo local del backend sin Docker

Requiere JDK 21 y una instancia de MySQL 8 corriendo (o usar solo el servicio `mysql` de compose: `docker compose up mysql`).

```bash
cd backend
./mvnw spring-boot:run
```

Variables de entorno esperadas en perfil `dev` (con defaults de conveniencia si no se setean): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`. En perfil `prod` son obligatorias, sin default.

## Tests

```bash
cd backend
./mvnw test
```

Los tests de integración usan **Testcontainers** (MySQL real, no H2) — requieren Docker corriendo y accesible. Si `./mvnw test` falla con `Could not find a valid Docker environment` a pesar de que `docker ps` funciona, revisar que Testcontainers pueda hablar con el daemon (en Windows con Docker Desktop, a veces requiere `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` o habilitar el acceso al socket por defecto en la configuración de Docker Desktop).

```bash
cd frontend
npm install
npm run dev     # servidor de desarrollo
npm run build   # build de producción
npm test        # Vitest + Testing Library
```

## Estado del scaffolding

**F1.3 — backend:**
- ✅ Proyecto Spring Boot con Web, Data JPA, Security (cadena mínima, sin JWT aún), Validation, Actuator, Lombok, MapStruct, springdoc-openapi, Flyway.
- ✅ Migración V1: `tenant`, `usuario`, `auditoria_log` (estructura, sin lógica de negocio encima).
- ✅ Multi-tenancy: `EntidadNegocio` (mapped superclass) + filtro Hibernate `tenantFilter` habilitado por request vía `TenantFilterInterceptor`. Tenant resuelto a un valor fijo (fila 1) hasta que F1.5 lo derive del JWT.
- ✅ Manejo de errores centralizado (`ProblemDetail` + catálogo de códigos, F1.1 §1.3).
- ✅ Docker Compose con `backend`, `frontend`, `mysql`, volumen persistente y healthchecks.

**F1.4 — frontend:**
- ✅ React 19 (satisface "React 18+" del brief) + TypeScript + Vite, Tailwind v4, shadcn/ui (preset propio a mano — ver nota abajo).
- ✅ Layout navegable: sidebar con los 10 módulos + header con placeholder de logo + `<Outlet/>`.
- ✅ React Query configurado (`QueryProvider`, Devtools solo en dev).
- ✅ React Hook Form + Zod con un formulario de ejemplo tipado (`/ejemplo-formulario`).
- ✅ Cliente HTTP tipado (`src/lib/http.ts`): inyección de JWT, refresh single-flight, redirección a `/login` en 401.
- ✅ Build de producción y tests (Vitest + Testing Library) verificados.

> **Nota sobre shadcn/ui:** el CLI (`npx shadcn@latest init`) falla en este entorno con `Could not load the workspace config` (bug reproducible incluso resolviendo la ruta del proyecto sin espacios). Los componentes base (`button`, `input`, `label`, `card`, `separator`) se bajaron igual del registro con `add` y se reubicaron a mano; `form.tsx` y el tema CSS (paleta `neutral` estándar en vez del preset "Nova") se escribieron a mano porque el registro no los sirvió completos. Funcionalmente equivalente; si se resuelve el bug del CLI más adelante, se puede re-correr `init` para alinear al preset original.

**F1.5 — autenticación, roles y autorización:**
- ✅ JWT access token (15 min por default) + refresh token opaco con rotación (7 días, hash SHA-256 persistido, nunca el valor crudo).
- ✅ `JwtAuthenticationFilter` puebla la autenticación y el `TenantContext` (F1.3) desde los claims — ya no queda fijo al tenant 1.
- ✅ Roles `ADMINISTRADOR/CARGA/LECTURA` con `@PreAuthorize`; endpoint representativo: gestión de usuarios (`/api/v1/usuarios`), admin-only.
- ✅ Invariante: no se puede desactivar ni degradar al último administrador activo (409).
- ✅ Frontend: pantalla de login, guard de rutas (`RequireAuth`), pantalla de usuarios (alta + activar/desactivar).
- ✅ Usuario admin sembrado por Flyway: `admin@montanaritech.com` / `changeme123` — **rotar antes de cualquier uso real**.
- ✅ Verificado en vivo (`docker compose up` + curl): login, `/auth/me`, 401 sin token, 403 por rol insuficiente, refresh con rotación (el token viejo queda inutilizable).

**F1.6 — infraestructura de auditoría:**
- ✅ `AuditoriaService.registrar(...)`: API única, síncrona en la misma transacción (F1.1 ADR-14).
- ✅ Anotación declarativa `@Auditado` (vía AOP) para el caso "solo importa el resultado" (alta de usuarios); llamada explícita de una línea para editar/activar/desactivar, que sí necesitan el "antes" para el diff.
- ✅ Pantalla de consulta (`/auditoria`, admin-only) con filtros por entidad, usuario, acción y rango de fechas.
- ✅ Verificado en vivo: crear + editar un usuario deja rastro con antes/después correctos y sin filtrar el hash de contraseña (`Usuario.passwordHash` lleva `@JsonIgnore`, defensa en profundidad).

### Bugs encontrados y corregidos en la verificación en vivo (no se detectan compilando)

- `BadCredentialsException` sin handler propio cayendo al catch-all → login con contraseña incorrecta devolvía 500 en vez de 401.
- `@PreAuthorize` deniega con `AuthorizationDeniedException`, que el catch-all `@ExceptionHandler(Exception.class)` interceptaba antes de que llegara al `AccessDeniedHandler` de Spring Security → 403 real devolvía 500. Ambos corregidos en `GlobalExceptionHandler`.

### Nota sobre el endpoint 401 por defecto

Cualquier endpoint que no sea Actuator health/info, Swagger o `/api/v1/auth/{login,refresh,logout}` requiere un access token válido (401 si falta o venció); los que además exigen rol admin devuelven 403 si el token es válido pero el rol no alcanza.

**F1.7 — CI básico:**
- ✅ GitHub Actions workflow (`.github/workflows/ci.yml`) en cada PR: build backend + tests, build frontend + tests.
- ✅ Cache de dependencias (Maven, npm) para acelerar CI.
- ✅ Badge de estado en el README.

**F1.8 — Molde de referencia (PL-1 a PL-5), checkpoint humano pendiente:**
- ✅ Guía de aplicación: [plan/00_plantillas_guia_aplicacion.md](plan/00_plantillas_guia_aplicacion.md) — léela antes de replicar el molde.
- ✅ PL-1/PL-2: CRUD completo de **Moneda** (backend `maestros/moneda/`, frontend `pages/monedas-page.tsx` con TanStack Table, ruta `/monedas`). Migración V3, tests unitarios (Mockito) + integración (Testcontainers) verdes, ciclo de vida completo verificado en vivo (crear, 409 por código duplicado, editar, activar/desactivar, eliminar, 400 por validación).
- ✅ PL-3: `ReportExportService` genérico (Excel vía POI streaming, PDF vía OpenPDF) + `ReporteMonedasController` de ejemplo. Excel y PDF verificados en vivo (`file` confirma ambos formatos válidos).
- ✅ PL-4: esqueleto `AsientoGenerator` (`common/asiento/`) — interfaz, líneas debe/haber, validador de balance, numerador (placeholder en memoria) + un generador de prueba que NO es una regla de negocio real.
- ✅ PL-5: `EstadoDocumento` (borrador/confirmado/anulado) + `TransicionEstadoValidator` (`common/estado/`), con tests de las 5 transiciones válidas/inválidas.
- ⚠️ **Checkpoint humano pendiente** (F1.8 lo exige): el equipo tiene que revisar el molde antes de que F2+ lo replique ~15 veces.

Un bug encontrado en la verificación en vivo (no en compilación ni en los tests con Testcontainers, que no corren en este entorno — ver nota más abajo): `HttpMessageNotReadableException` (body JSON inválido, p. ej. bytes no-UTF-8) caía en el catch-all genérico y devolvía 500 en vez de 400. Corregido en `GlobalExceptionHandler`.

**F2.1 — CRUDs simples en lote (batch 1 de maestros):**
- ✅ 6 CRUDs completos: TipoCambio (FK a Moneda), Jurisdiccion, Categoria (con enum tipo), Rubro (FK a Categoria), Concepto (FK opcional a Moneda), TipoCosto. Migraciones V4-V9.
- ✅ Backend: entidades/repositorios/DTOs/mappers/services/controllers, 23 tests unitarios (Mockito) verdes.
- ✅ Frontend: 6 páginas con TanStack Table + RHF/Zod + React Query, rutas y nav agregadas (reemplaza el placeholder genérico de "Maestros").
- ✅ Ciclo de vida completo (crear/listar/editar/activar-desactivar/eliminar) y las dos FKs (Rubro→Categoria, TipoCambio→Moneda) verificados en vivo, incluyendo 404 con FK inexistente.

Esta ronda la corrió primero Haiku 4.5 (según el plan) y quedó con **3 bugs de compilación reales** que Sonnet 5 tuvo que encontrar y corregir antes del primer build exitoso — quedan documentados porque son la clase de error que se repetirá si el molde se sigue aplicando sin revisar la salida real del compilador:
- `JurisdiccionService`/`RubroService` llamaban a `req.descripcion()`/`e.setDescripcion(...)` que no existen en esos DTOs/entidades (el generador copió el cuerpo genérico del molde sin adaptarlo a los campos reales de cada entidad — Jurisdiccion no tiene descripción, tiene código y alícuota; Rubro no tiene descripción, tiene categoría FK y orden).
- `TipoCambioService` llamaba a `tc.setMonedaId(...)`, pero `TipoCambio.moneda` es una relación `@ManyToOne`, no un campo `Long` — había que resolver la entidad `Moneda` vía su repositorio antes de asignarla.
- `Jurisdiccion.alicuotaIIBB` (acrónimo en mayúsculas) generaba un nombre de columna físico distinto al de la migración SQL bajo la estrategia de naming por defecto de Hibernate — solo se manifestó como `SchemaManagementException` al levantar el contenedor real, no en `mvn compile`. Se corrigió con `@Column(name = "alicuota_iibb")` explícito.

En el frontend, `z.coerce.number()` (Zod v4) resultó incompatible con la inferencia de tipos de `useForm` (`@hookform/resolvers` v5 + `react-hook-form` v7): rompía el build de TypeScript en las 3 páginas con selects de FK numérica. Se resolvió modelando esos campos como `string` en el schema de Zod y convirtiendo a `Number(...)` recién en el `onSubmit`, evitando el desajuste de tipos input/output que introduce `coerce`.

**F2.2 — Cliente** y **F2.3 — Proveedor** (validación de CUIT con dígito verificador, FK a Jurisdicción, M2M a TipoCosto):
- ✅ CRUD completo de **Cliente**: nombre, CUIT, FK a Jurisdicción, contacto/email/teléfono opcionales. Migración V10.
- ✅ CRUD completo de **Proveedor**: igual que Cliente + FK opcional a Moneda habitual + relación N:M a TipoCosto (tabla `proveedor_tipo_costo`). Migración V11.
- ✅ Validación de CUIT real (no solo el formato `XX-XXXXXXXX-X`): `CuitValido`/`CuitValidoValidator` (`common/validation/`) implementan el algoritmo del dígito verificador (módulo 11, pesos `5,4,3,2,7,6,5,4,3,2`) — un CUIT con formato correcto pero dígito verificador incorrecto ahora devuelve 400, verificado en vivo.
- ✅ Backend: 36 tests unitarios nuevos (16 Cliente + 20 Proveedor), frontend: `clientes-page.tsx` y `proveedores-page.tsx` (esta última con selector de moneda y checkboxes para el M2M de tipos de costo).
- ✅ Ciclo de vida completo, ambas FKs y el M2M verificados en vivo contra MySQL real.

Esta ronda también la corrió primero Haiku 4.5, y Sonnet 5 volvió a encontrar bugs reales antes de dar el trabajo por terminado — el patrón se repite ronda tras ronda, así que conviene asumir que **ninguna ronda de Haiku queda lista sin una pasada de Sonnet que compile, testee y levante el stack de verdad**:
- `ClienteService`/`ProveedorService` importaban `JurisdictionRepository` (nombre en inglés que no existe) en vez de `JurisdiccionRepository` — error de compilación en 2 servicios + 2 tests.
- 6 tests (`listarSinFiltrosRetornaTodos`, `listarConTextoFiltra`, `listarConActivoFiltra` en ambos) mezclaban el matcher `any()` con valores `null`/`true` crudos en la misma llamada a Mockito (`InvalidUseOfMatchers`) — cuando se usa un matcher, *todos* los argumentos de esa llamada tienen que ser matchers (`isNull()`, `eq(true)`).
- La validación de CUIT que pedía el plan original ("formato XX-XXXXXXXX-X con dígito verificador") había quedado como un `@Pattern` de solo-formato — sin chequeo de dígito verificador real. Se agregó el validador dedicado arriba mencionado.
- El hook `use-tipos-costo.ts` (nuevo, para el selector M2M de Proveedor) llamaba a `/tipos-costo` — la ruta real (ya usada por el hook de F2.1) es `/tipo-costos`. Se eliminó el hook duplicado y se reutilizó `useTipoCostos` de `use-tipocosto.ts`.
- El campo `email` opcional en ambos formularios usaba `z.string().email().optional()` con valor por defecto `""`: Zod trata `.optional()` como "permite `undefined`", no "permite string vacío", así que dejar el campo en blanco fallaba la validación de email. Se corrigió con `z.union([z.string().email().max(100), z.literal("")]).optional()`.
- Bug propio (no de Haiku): las migraciones V10/V11 que yo mismo escribí antes de delegar a Haiku usaban `fecha_creacion`/`fecha_actualizacion` y omitían `creado_por`/`actualizado_por`/`version` — no coincidían con las columnas reales que espera `EntidadNegocio` (`creado_en`, `creado_por`, `actualizado_en`, `actualizado_por`, `version`). Solo se manifestó como `SchemaManagementException` al levantar el contenedor, igual que el bug de `alicuotaIIBB` en F2.1 — otro recordatorio de que la validación de esquema de Hibernate solo se dispara en runtime, nunca en `mvn compile`.
