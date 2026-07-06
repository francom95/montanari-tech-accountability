# [F3.6] Mayores contables

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 21 de 55 |
| **ID** | F3.6 |
| **Fase** | F3 — Núcleo contable |
| **Modelo** | Sonnet 5 |
| **Depende de** | F3.4 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Mayor por cuenta: fecha, número de asiento, descripción, debe, haber, saldo acumulado línea a línea, saldo final con indicación deudor/acreedor.
- Filtros: fecha/período, cuenta, rubro, proyecto, cliente, proveedor, tipo de operación, moneda, origen del movimiento.
- Acceso directo desde el árbol del plan de cuentas ('ver mayor').
- Exportación Excel y PDF aplicando PL-3 (usar el `ReportExportService`; si aún es embrionario, extenderlo aquí).
- Cuidar performance: query con índices adecuados, paginación en pantalla, export completo en streaming.

### Entradas que deben adjuntarse a la sesión

- Motor F3.4.
- PL-3 de F1.8.

### Salida esperada (definición de terminado)

- Mayor consultable, filtrable y exportable desde cada cuenta.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
