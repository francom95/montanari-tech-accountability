# [F5.1] Movimientos bancarios y bandeja 'pendiente de revisar'

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 28 de 55 |
| **ID** | F5.1 |
| **Fase** | F5 — Bancos y conciliación |
| **Modelo** | Sonnet 5 |
| **Depende de** | F4.4 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Entidad **Movimiento bancario** ligada a cuenta (F2.4): fecha, descripción original, importe, moneda, referencia, origen de importación, estado.
- Flujo de estados de importación: `pendiente de revisar` → acciones: **confirmar** (impacta contabilidad vía asiento), **asociar** (matchear con cobro/pago/asiento existente), **imputar** (elegir cuenta contable y generar asiento), **descartar**, **corregir**.
- NADA impacta la contabilidad hasta que el usuario lo confirme explícitamente.
- Bandeja UI: lista de pendientes por cuenta con acciones rápidas, contador de pendientes visible (alimentará alertas F9.1).
- Auditoría de cada decisión sobre un movimiento.

### Entradas que deben adjuntarse a la sesión

- Cuentas F2.4.
- Motor de asientos F3.4.
- Cobros/pagos F4.4.

### Salida esperada (definición de terminado)

- Bandeja de pendientes operativa con las 5 acciones y sin impacto contable automático.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Las importaciones bancarias entran como 'pendientes de revisar'; nunca crean movimientos definitivos automáticamente.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
