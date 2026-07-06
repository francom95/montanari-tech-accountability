# [F11.4] Documentación de usuario

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 55 de 55 |
| **ID** | F11.4 |
| **Fase** | F11 — Hardening y revisión final |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F11.3 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Manual de uso por módulo, orientado al equipo administrativo y al contador (no técnico):
- Carga de facturas de venta y compra (y qué asientos generan); registro de cobros y pagos (incluido USD y diferencias de cambio en términos simples); importación de resúmenes bancarios y trabajo con la bandeja de pendientes; conciliación paso a paso; liquidación de IVA e IIBB (edición y confirmación); cierre de períodos; calendario de vencimientos y alertas; presupuestos de proyectos y lectura del reporte de rentabilidad; búsqueda Lupita; pendientes administrativos; exportaciones.
- Formato: un MD por módulo con capturas descriptas (placeholders donde vayan imágenes) + un PDF compilado.
- Tono claro y directo, en castellano rioplatense, sin jerga técnica.
- Incluir sección de preguntas frecuentes con los 10 errores de carga más probables y cómo corregirlos.

### Entradas que deben adjuntarse a la sesión

- Sistema final desplegado.
- Guías internas de cada fase si existen.

### Salida esperada (definición de terminado)

- Manual completo en MD + PDF compilado.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
