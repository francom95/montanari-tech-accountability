# [F2.1] CRUDs simples en lote (batch 1 de maestros)

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 9 de 55 |
| **ID** | F2.1 |
| **Fase** | F2 — Maestros |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F1.8 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-1, PL-2 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Aplicar PL-1 + PL-2, copiando el molde de Moneda (F1.8), a estas 6 entidades:
- 1. **Tipo de cambio**: fecha, moneda, criterio (BNA venta / BNA compra / oficial / manual / otro), valor compra, valor venta, fuente, observaciones.
- 2. **Jurisdicción impositiva**: nombre (Buenos Aires, CABA, Córdoba, ...), código, alícuota IIBB por actividad (lista), activo.
- 3. **Categoría contable**: nombre, descripción, tipo (Activo/Pasivo/PN/R+/R-), activo.
- 4. **Rubro**: nombre, categoría contable asociada, orden, activo.
- 5. **Concepto recurrente**: nombre, descripción, categoría/cuenta sugerida, periodicidad, importe estimado, moneda.
- 6. **Tipo de costo**: nombre, descripción, activo. Debe permitir crear tipos nuevos por el usuario (diseño, programación, edición, administración, contador, comisiones, suscripciones, impuestos, servicios profesionales, infraestructura, software, otros).
- Cada entidad: migración Flyway propia, tests del happy path, pantalla en el sidebar bajo 'Maestros'.

### Entradas que deben adjuntarse a la sesión

- Código molde de F1.8 (adjuntar).
- Guía de plantillas de F1.8 (adjuntar).
- Archivo 00_plantillas.md (adjuntar).

### Salida esperada (definición de terminado)

- 6 CRUDs completos back+front, compilando, con tests verdes.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Nota para el operador

Replicá el molde SIN modificar el patrón. Si un campo no encaja, reportalo; no inventes estructura nueva.
