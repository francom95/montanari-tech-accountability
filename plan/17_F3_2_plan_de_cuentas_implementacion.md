# [F3.2] Plan de cuentas (implementación)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 17 de 55 |
| **ID** | F3.2 |
| **Fase** | F3 — Núcleo contable |
| **Modelo** | Sonnet 5 |
| **Depende de** | F3.1 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Implementar el plan de cuentas según la spec de F3.1: jerarquía madre/imputable, código único, tipo, rubro, tipo de saldo esperado, activo, proyectos asociados.
- Bloqueo absoluto de imputación en cuentas madre (validación en service, no solo en UI).
- Creación de nuevas categorías, rubros y cuentas sin límite del plan inicial.
- UI: árbol expandible con búsqueda, alta/edición inline o modal, indicador de imputable/madre.
- Tests: intentar imputar a cuenta madre falla; mover cuenta de rama recalcula jerarquía.

### Entradas que deben adjuntarse a la sesión

- Spec F3.1.
- Categorías y rubros de F2.1.

### Salida esperada (definición de terminado)

- Plan de cuentas administrable con jerarquía funcionando.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Las cuentas madre solo agrupan: no son imputables. Solo cuentas imputables reciben movimientos.
