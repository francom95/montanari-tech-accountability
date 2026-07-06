# [F9.3] Períodos contables y cierre

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 48 de 55 |
| **ID** | F9.3 |
| **Fase** | F9 — Transversales |
| **Modelo** | Sonnet 5 |
| **Depende de** | F3.5 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Implementar períodos mensuales según el diseño de F3.1: entidad Período con estado (abierto / en revisión / cerrado).
- **El cierre NO bloquea**: consultar, importar, exportar y visualizar siguen funcionando siempre.
- El cierre SÍ impide modificaciones accidentales: crear/editar/anular asientos, facturas, cobros o pagos con fecha dentro de un período cerrado requiere rol admin + confirmación explícita + auditoría reforzada (motivo obligatorio).
- Pantalla de gestión de períodos (solo admin): cerrar, reabrir, ver qué liquidaciones/conciliaciones tiene asociadas.
- Advertencia no bloqueante al liquidar impuestos sobre un período aún abierto.
- Tests: usuario de carga no puede tocar período cerrado; admin sí con motivo; consultas nunca bloqueadas.

### Entradas que deben adjuntarse a la sesión

- Diseño F3.1.
- Auditoría F1.6.
- Asientos F3.5.

### Salida esperada (definición de terminado)

- Gestión de períodos con cierre como control, no como bloqueo.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- El cierre de período evita modificaciones accidentales pero no bloquea consultas, importaciones ni exportaciones. Modificar un período cerrado requiere rol admin y auditoría reforzada.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
