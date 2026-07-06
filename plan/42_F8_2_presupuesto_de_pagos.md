# [F8.2] Presupuesto de pagos

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 42 de 55 |
| **ID** | F8.2 |
| **Fase** | F8 — Presupuesto, vencimientos y caja |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F8.1 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-1, PL-2 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Aplicar PL-1 + PL-2 a **Compromiso de pago futuro**: concepto, tipo (cuota de plan de pagos, vencimiento impositivo, IVA diferido, IIBB, pago a proveedor, sueldos, cargas sociales, contador, comisión bancaria, comisión por venta, suscripción, tarjeta, otro egreso), fecha prevista, importe, moneda, proveedor asociado (opcional), proyecto asociado (opcional), estado, observaciones.
- Vínculo con calendario: al crear un compromiso puede generar su vencimiento en F8.1 (checkbox).
- Los compromisos alimentan el flujo proyectado (F8.3): exponer un query service simple 'compromisos por rango de fechas'.
- No implementar lógica de proyección acá: solo el CRUD + el query service.

### Entradas que deben adjuntarse a la sesión

- Molde F1.8 + 00_plantillas.md.
- Calendario F8.1.

### Salida esperada (definición de terminado)

- CRUD de compromisos + query service para la proyección.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
