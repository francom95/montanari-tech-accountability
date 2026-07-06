# [F3.5] Búsqueda, duplicación, edición y anulación de asientos

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 20 de 55 |
| **ID** | F3.5 |
| **Fase** | F3 — Núcleo contable |
| **Modelo** | Sonnet 5 |
| **Depende de** | F3.4 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Búsqueda/filtrado de asientos por fecha, número, leyenda, cuenta, importe, proyecto, cliente, proveedor, origen, estado.
- Duplicación: copiar un asiento existente y modificar libremente fecha, leyenda, cuentas, importes, proyecto antes de guardar.
- Edición según permisos (incluye asientos automáticos una vez que existan, F4).
- Anulación según la regla decidida en F3.1 (contra-asiento o marca), conservando trazabilidad.
- Todo cambio auditado vía F1.6.
- Tests de permisos: usuario de solo lectura no puede editar; usuario de carga no puede eliminar.

### Entradas que deben adjuntarse a la sesión

- Motor F3.4.
- Reglas de anulación de F3.1.

### Salida esperada (definición de terminado)

- Ciclo de vida completo del asiento operativo y auditado.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Los asientos automáticos son editables después de generados; toda edición queda en auditoría.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
