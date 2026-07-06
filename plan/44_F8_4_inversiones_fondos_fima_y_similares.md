# [F8.4] Inversiones (Fondos Fima y similares)

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 44 de 55 |
| **ID** | F8.4 |
| **Fase** | F8 — Presupuesto, vencimientos y caja |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F8.3 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-1, PL-2 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Aplicar PL-1 + PL-2 a **Inversión**: instrumento (Fondo Fima u otro), cuenta de origen (FK a F2.4), y sus movimientos 1:N: tipo (suscripción/rescate), fecha, monto aplicado, cuotapartes, valor de cuotaparte, fecha de liquidación.
- Campos de la inversión: valuación actual (cuotapartes × último valor cargado), rendimiento (valuación − neto aplicado), objetivo del dinero (texto: ej. 'IVA marzo'), relación con pagos futuros (FK opcional a compromiso F8.2 o vencimiento F8.1), estado.
- Los cálculos son aritmética simple con BigDecimal: no hay lógica financiera compleja.
- El saldo invertido se refleja en el flujo proyectado (F8.3 ya lo consume vía query service: exponerlo).
- Suscripción/rescate genera movimiento en la cuenta de origen (vía servicios de F4.4/F5.1, no directo).

### Entradas que deben adjuntarse a la sesión

- Molde F1.8 + 00_plantillas.md.
- Cuentas F2.4.
- Flujo F8.3.

### Salida esperada (definición de terminado)

- Registro de inversiones con valuación y vínculo a obligaciones futuras.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
