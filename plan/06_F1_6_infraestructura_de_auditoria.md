# [F1.6] Infraestructura de auditoría

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 6 de 55 |
| **ID** | F1.6 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Sonnet 5 |
| **Depende de** | F1.5 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Implementar el esquema de auditoría diseñado en F1.1: listener/aspecto que registra usuario, fecha/hora, acción, entidad, ID, datos anteriores y nuevos (JSON) en cada operación sensible.
- Aplicable de forma declarativa (anotación o interceptor) para que los CRUDs posteriores lo hereden sin código extra.
- Pantalla de consulta de auditoría (solo admin): filtros por entidad, usuario, fecha, acción.
- Tests: crear/editar/eliminar una entidad de prueba deja rastro completo y correcto.

### Entradas que deben adjuntarse a la sesión

- Diseño de auditoría de F1.1.
- F1.5 (para atribuir usuario).

### Salida esperada (definición de terminado)

- Toda escritura sensible deja rastro consultable sin esfuerzo adicional por entidad.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
