# [F8.3] Flujo de caja real y proyectado

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 43 de 55 |
| **ID** | F8.3 |
| **Fase** | F8 — Presupuesto, vencimientos y caja |
| **Modelo** | Sonnet 5 |
| **Depende de** | F8.2 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- **Flujo real** por cuenta y consolidado: saldo inicial (F2.4) + ingresos cobrados − egresos pagados ± movimientos de inversión ± financiación = saldo final; por período, con detalle diario/semanal/mensual.
- **Flujo proyectado**: partir del saldo real actual y proyectar con: cobros esperados (cuotas de proyectos F2.5 pendientes), compromisos (F8.2), vencimientos (F8.1: IVA diferido, IIBB, anticipos de Ganancias, sueldos, cargas sociales, honorarios, comisiones, suscripciones, tarjetas), pagos a proveedores pendientes (CxP F4.5).
- Vista combinada real+proyectado con línea de 'hoy'; detección de días con saldo proyectado negativo (alimenta alertas F9.1).
- Multimoneda: consolidado en ARS con TC configurable de proyección; detalle por moneda.
- Exportable (F7.1) — completa el pendiente de la sección 7.9.

### Entradas que deben adjuntarse a la sesión

- Saldos F2.4/F5.3.
- Cobros/pagos F4.4.
- Compromisos F8.2.
- Vencimientos F8.1.
- Cuotas de proyectos F2.5.

### Salida esperada (definición de terminado)

- Flujo real y proyectado exportables con detección de saldos negativos futuros.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Las cuentas bancarias/dinero/tarjetas tienen saldo inicial con fecha, editable, desde el cual se calcula la evolución.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
