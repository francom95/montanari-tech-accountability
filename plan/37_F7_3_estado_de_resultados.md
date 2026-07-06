# [F7.3] Estado de resultados

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 37 de 55 |
| **ID** | F7.3 |
| **Fase** | F7 — Reportes y dashboard |
| **Modelo** | Sonnet 5 |
| **Depende de** | F7.2 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- ER con la apertura: ingresos por ventas; otros ingresos por ventas; costos de prestación de servicios; gastos de comercialización; gastos de administración; gastos financieros; impuestos; otros ingresos; otros egresos. Subtotales: resultado bruto, operativo, final.
- Vistas: por **mes**, por **año**, **acumulado**, y por **proyecto** (usando la asociación de asientos a proyectos).
- Comparativo simple mes vs mes anterior (variación absoluta y %).
- El mapeo rubro→línea del ER debe ser configurable (tabla de parametrización), no hardcodeado.
- Exportable (F7.1); drill-down de cada línea a las cuentas que la componen.

### Entradas que deben adjuntarse a la sesión

- Asientos F3.4.
- Export F7.1.

### Salida esperada (definición de terminado)

- ER exportable con las 4 vistas y mapeo configurable.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
