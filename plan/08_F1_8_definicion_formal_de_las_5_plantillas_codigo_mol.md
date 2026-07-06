# [F1.8] Definición formal de las 5 plantillas (código molde)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 8 de 55 |
| **ID** | F1.8 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Sonnet 5 |
| **Depende de** | F1.6 |
| **Checkpoint humano** | Sí — el equipo revisa el molde antes de replicarlo ~15 veces. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Implementar la entidad ejemplo **Moneda** COMPLETA aplicando PL-1 (backend) y PL-2 (frontend). Este código es el molde literal que Haiku copiará en Fase 2.
- PL-1 backend: Entity JPA (con `tenant_id`, `activo`, timestamps) + Repository + Service + Controller REST + DTOs + MapStruct + Bean Validation + migración Flyway + listar paginado con filtros + activar/desactivar (soft-delete) + eliminar solo sin movimientos asociados (409 con mensaje claro) + OpenAPI + test unitario de service + test de integración Testcontainers.
- PL-2 frontend: ruta + TanStack Table (paginación, orden, filtros, búsqueda) + formulario RHF+Zod espejando validaciones + hooks React Query (list/get/create/update/toggle/delete) + manejo de 409 + estados de carga.
- PL-3: un reporte dummy exportable (endpoint con filtros estándar + `ReportExportService` embrionario con POI y OpenPDF + botones de exportación).
- PL-4: esqueleto de la interfaz `AsientoGenerator` (evento → líneas debe/haber → validación de balance → numeración interna → vínculo a documento origen → editable con auditoría). Solo esqueleto + un generator de prueba.
- PL-5: enum de estados borrador/confirmado/anulado + transiciones válidas (borrador→confirmado, confirmado→anulado; anulado terminal) + auditoría de cambios de estado.
- Escribir una guía de 1 página: 'cómo aplicar cada plantilla a una entidad nueva' (será el prompt base para Haiku).

### Entradas que deben adjuntarse a la sesión

- Arquitectura F1.1.
- Auditoría F1.6 funcionando.

### Salida esperada (definición de terminado)

- Código molde compilando con tests verdes + guía de aplicación de plantillas.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el equipo revisa el molde antes de replicarlo ~15 veces.

### Reglas de negocio que NO se pueden romper en este paso

- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
- Toda entidad de negocio incluye `tenant_id` y filtro correspondiente, aunque la UI no muestre multiempresa.
- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.

### Nota para el operador

La calidad de este molde determina la calidad de todo lo que Haiku produzca. Cuidá nombres, comentarios mínimos y consistencia: será copiado literalmente.
