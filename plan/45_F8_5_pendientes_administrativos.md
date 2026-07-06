# [F8.5] Pendientes administrativos

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 45 de 55 |
| **ID** | F8.5 |
| **Fase** | F8 — Presupuesto, vencimientos y caja |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F2.5 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-1, PL-2 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Aplicar PL-1 + PL-2 a **Pendiente administrativo**: título, descripción, fecha de creación (auto), fecha estimada de resolución, prioridad (alta/media/baja), estado (pendiente / en proceso / resuelto / cancelado / postergado), responsable (usuario), categoría, proyecto asociado (opcional), cliente asociado (opcional), proveedor asociado (opcional), observaciones.
- UI: lista con filtros por estado/prioridad/responsable/categoría + vista tipo tablero simple por estado (opcional si la biblioteca UI lo facilita; si no, lista alcanza).
- Los pendientes próximos a vencer alimentan alertas (F9.1): exponer query service 'pendientes por vencer en N días'.
- Casos de uso que debe soportar (del documento fuente): recordatorios, controles manuales, revisiones del contador, ajustes pendientes, facturas a pedir, pagos a verificar, movimientos bancarios a identificar, impuestos a revisar.

### Entradas que deben adjuntarse a la sesión

- Molde F1.8 + 00_plantillas.md.
- Usuarios F1.5, Proyectos F2.5.

### Salida esperada (definición de terminado)

- Pantalla de pendientes operativa con query service para alertas.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
