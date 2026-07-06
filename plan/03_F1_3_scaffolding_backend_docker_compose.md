# [F1.3] Scaffolding backend + Docker Compose

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 3 de 55 |
| **ID** | F1.3 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Sonnet 5 |
| **Depende de** | F1.2 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Crear el proyecto Spring Boot 3.x / Java 21 con: Web, Data JPA, Security, Validation, Actuator, Lombok, MapStruct, springdoc-openapi, Flyway.
- Docker Compose con servicios `backend`, `frontend`, `mysql` (8.x), volúmenes persistentes, healthchecks.
- Perfiles `dev` y `prod` (application-dev.yml / application-prod.yml); secretos vía variables de entorno / `.env` (nunca hardcodeados).
- Migración Flyway V1 con la estructura base definida en F1.1: tenant, usuarios, roles, tabla de auditoría.
- README con instrucciones de arranque.

### Entradas que deben adjuntarse a la sesión

- ADRs de F1.2.
- Esquema base de F1.1.

### Salida esperada (definición de terminado)

- `docker compose up` levanta todo el entorno; Actuator health OK; Swagger UI accesible.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Toda entidad de negocio incluye `tenant_id` y filtro correspondiente, aunque la UI no muestre multiempresa.
