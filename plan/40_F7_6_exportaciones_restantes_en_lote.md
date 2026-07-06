# [F7.6] Exportaciones restantes en lote

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 40 de 55 |
| **ID** | F7.6 |
| **Fase** | F7 — Reportes y dashboard |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F7.1, F6.2 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Aplicar PL-3 (con el `ReportExportService` de F7.1) a los listados que faltan de la sección 7.9: **clientes**, **proveedores**, **movimientos contables** (libro diario), **IVA** (liquidaciones), **IIBB** (liquidaciones).
- Cada uno: endpoint con filtros estándar + botones Excel/PDF en la pantalla existente.
- No crear pantallas nuevas: agregar la exportación a las existentes.
- Verificar contra la lista de la sección 7.9 que no falte ninguno: mayores, balance, ER, por proyecto, CxC, CxP, IVA, IIBB, flujo de caja (F8.3 lo hará), clientes, proveedores, movimientos.

### Entradas que deben adjuntarse a la sesión

- Export F7.1.
- Pantallas existentes.

### Salida esperada (definición de terminado)

- Todos los exports de la sección 7.9 disponibles (salvo flujo de caja, que llega con F8.3).
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
