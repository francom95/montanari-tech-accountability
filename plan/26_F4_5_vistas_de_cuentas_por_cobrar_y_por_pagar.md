# [F4.5] Vistas de cuentas por cobrar y por pagar

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 26 de 55 |
| **ID** | F4.5 |
| **Fase** | F4 — Facturación, cobros y pagos |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F4.4 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Aplicar PL-3 a dos vistas de solo lectura sobre datos ya calculados por F4.4:
- **Cuentas por cobrar**: cliente, proyecto, factura, fecha, vencimiento, importe, moneda, cobros parciales, saldo pendiente, estado.
- **Cuentas por pagar**: proveedor, factura, fecha, vencimiento, importe, moneda, pagos parciales, saldo pendiente, estado.
- Filtros estándar de PL-3 + filtro por vencido/por vencer.
- Exportación Excel y PDF con el servicio común.
- Totales al pie por moneda.

### Entradas que deben adjuntarse a la sesión

- Datos de F4.4.
- PL-3 y `ReportExportService`.

### Salida esperada (definición de terminado)

- CxC y CxP consultables, filtrables y exportables.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Nota para el operador

No recalcules saldos: consumí los servicios de F4.4.
