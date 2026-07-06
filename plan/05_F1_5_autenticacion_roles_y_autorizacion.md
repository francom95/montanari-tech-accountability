# [F1.5] Autenticación, roles y autorización

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 5 de 55 |
| **ID** | F1.5 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Sonnet 5 |
| **Depende de** | F1.3, F1.4 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Spring Security con JWT access + refresh; BCrypt para passwords.
- Roles: administrador, usuario de carga, usuario de solo lectura. Guards por endpoint (`@PreAuthorize` o equivalente).
- Reglas de negocio de permisos: solo admin cierra períodos, edita asientos automáticos, elimina registros, administra usuarios y configuraciones sensibles.
- Pantallas: login, y gestión de usuarios (solo admin) usando el molde de CRUD si ya existe o dejándolo alineado a lo que será PL-1/PL-2.
- Interceptor frontend para token y manejo de expiración.
- Tests: acceso permitido/denegado por rol en endpoints representativos.

### Entradas que deben adjuntarse a la sesión

- Diseño de seguridad de F1.1.
- Scaffoldings F1.3 y F1.4.

### Salida esperada (definición de terminado)

- Login funcional; matriz de permisos verificada por tests.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
