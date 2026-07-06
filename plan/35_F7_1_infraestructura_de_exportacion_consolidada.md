# [F7.1] Infraestructura de exportación consolidada

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 35 de 55 |
| **ID** | F7.1 |
| **Fase** | F7 — Reportes y dashboard |
| **Modelo** | Sonnet 5 |
| **Depende de** | F3.6 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Consolidar el `ReportExportService` común iniciado en F1.8/F3.6: API única para exportar cualquier reporte a **Excel (Apache POI)** y **PDF (OpenPDF)**.
- Estilos corporativos centralizados: encabezado con nombre de empresa + placeholder de logo (el logo llegará después: dejar un slot de imagen configurable en parámetros del sistema), título de reporte, filtros aplicados, fecha de emisión, numeración de páginas en PDF, formatos de moneda AR ($ 1.234.567,89) y fechas dd/mm/yyyy.
- Export en streaming para volúmenes grandes (mayores completos).
- Refactorizar los exports ya hechos (F3.6, F4.5) para que usen esta versión consolidada.
- Test: export de 50k filas sin OOM.

### Entradas que deben adjuntarse a la sesión

- Exports existentes F3.6/F4.5.
- PL-3.

### Salida esperada (definición de terminado)

- Servicio único de exportación usado por todos los reportes, con estilos consistentes.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
