# [F1.7] CI básico

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 7 de 55 |
| **ID** | F1.7 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F1.3, F1.4 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- GitHub Actions: workflow que en cada PR ejecuta build backend + tests (con Testcontainers si está disponible en runner, o perfil H2/MySQL service) y build frontend + tests (Vitest).
- Cache de dependencias (Maven/Gradle y npm).
- Badge de estado en el README.

### Entradas que deben adjuntarse a la sesión

- Repos de F1.3 y F1.4.

### Salida esperada (definición de terminado)

- Pipeline verde en un PR de prueba.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Nota para el operador

Configuración estándar; no agregues pasos de deploy todavía.
