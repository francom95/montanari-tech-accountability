# [F9.1] Motor de alertas

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 46 de 55 |
| **ID** | F9.1 |
| **Fase** | F9 — Transversales |
| **Modelo** | Sonnet 5 |
| **Depende de** | F8.1 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Job programado (Spring @Scheduled) que evalúa reglas y genera alertas persistidas con: tipo, severidad, mensaje, entidad vinculada, fecha, leída/no leída.
- Las 13 reglas de la sección 12: vencimientos impositivos próximos; pagos pendientes; facturas sin cobrar (atraso configurable); saldos bajos (umbral por cuenta); vencimientos de tarjetas; impuestos a pagar; obligaciones vencidas; cobros atrasados; pagos próximos a proveedores; vencimientos de planes de pago; movimientos bancarios pendientes de revisar; diferencias en conciliaciones; pendientes administrativos próximos a vencer.
- Umbrales y días de anticipación configurables por el admin (tabla de parámetros).
- UI: badge con contador en el header + panel de alertas + zona en el dashboard (slot de F7.5).
- Arquitectura lista para email en etapa 2: interfaz `AlertChannel` con implementación `InApp` hoy, `Email` mañana.

### Entradas que deben adjuntarse a la sesión

- Vencimientos F8.1.
- CxC/CxP F4.5.
- Bandeja F5.1.
- Pendientes F8.5.
- Dashboard F7.5.

### Salida esperada (definición de terminado)

- Alertas automáticas visibles en dashboard y header, con umbrales configurables.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
