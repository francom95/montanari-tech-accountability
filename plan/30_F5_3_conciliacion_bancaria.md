# [F5.3] Conciliación bancaria

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 30 de 55 |
| **ID** | F5.3 |
| **Fase** | F5 — Bancos y conciliación |
| **Modelo** | Sonnet 5 |
| **Depende de** | F5.2 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Pantalla de conciliación por cuenta y período: movimientos del banco (importados) vs movimientos del sistema (asientos/cobros/pagos).
- **Matching sugerido** automático: importe exacto + fecha con tolerancia configurable (±3 días); el usuario confirma o rechaza cada sugerencia.
- Marcar movimientos conciliados; detectar y listar diferencias.
- Identificación asistida con imputación rápida (un clic → asiento con cuenta preconfigurada, usando el mapeo de F4.1) para: comisiones bancarias, impuestos bancarios (ley 25.413), SIRCREB, percepciones, débitos y créditos no registrados.
- Resumen de conciliación: saldo banco vs saldo sistema, partidas conciliatorias, estado por período.
- La automatización avanzada queda para etapa 2: acá el usuario siempre decide.

### Entradas que deben adjuntarse a la sesión

- Movimientos F5.1/F5.2.
- Mapeo de cuentas F4.1.

### Salida esperada (definición de terminado)

- Conciliación operativa por cuenta y período con matching sugerido e imputación rápida.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Las importaciones bancarias entran como 'pendientes de revisar'; nunca crean movimientos definitivos automáticamente.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
