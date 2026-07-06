# Montanari Tech — Sistema de Gestión Contable

Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech. Ver [CLAUDE.md](CLAUDE.md) y `./plan` para el proceso de desarrollo orquestado; este README es sobre el **sistema en sí** (`./backend`, `./frontend`).

## Estructura

```
backend/    Java 21 + Spring Boot 3.x (Maven)
frontend/   placeholder estático — el scaffolding real de F1.4 lo reemplaza
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
- Frontend (placeholder hasta F1.4): http://localhost:5173
- MySQL expuesto en `localhost:3307` (puerto host no estándar para no chocar con otra instancia local; usuario/clave de `.env`)

Flyway corre las migraciones automáticamente al arrancar el backend.

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

## Estado del scaffolding (F1.3)

- ✅ Proyecto Spring Boot con Web, Data JPA, Security (cadena mínima, sin JWT aún), Validation, Actuator, Lombok, MapStruct, springdoc-openapi, Flyway.
- ✅ Migración V1: `tenant`, `usuario`, `auditoria_log` (estructura, sin lógica de negocio encima).
- ✅ Multi-tenancy: `EntidadNegocio` (mapped superclass) + filtro Hibernate `tenantFilter` habilitado por request vía `TenantFilterInterceptor`. Tenant resuelto a un valor fijo (fila 1) hasta que F1.5 lo derive del JWT.
- ✅ Manejo de errores centralizado (`ProblemDetail` + catálogo de códigos, F1.1 §1.3).
- ✅ Docker Compose con `backend`, `frontend` (placeholder), `mysql`, volumen persistente y healthchecks.
- ⏳ **Pendiente de pasos futuros**: JWT/roles/login (F1.5), servicio y pantalla de auditoría (F1.6), CI (F1.7), scaffolding real del frontend (F1.4).

### Nota sobre el endpoint 401 por defecto

Con la seguridad mínima de este paso, **cualquier endpoint que no sea Actuator health/info o Swagger devuelve 401**, porque todavía no existe ningún mecanismo de autenticación (JWT llega en F1.5). Es el comportamiento esperado, no un bug.
