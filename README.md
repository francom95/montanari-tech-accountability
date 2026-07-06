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
