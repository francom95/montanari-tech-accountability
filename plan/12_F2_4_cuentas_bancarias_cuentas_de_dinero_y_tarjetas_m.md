# [F2.4] Cuentas bancarias, cuentas de dinero y tarjetas (maestros con saldo inicial)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 12 de 55 |
| **ID** | F2.4 |
| **Fase** | F2 — Maestros |
| **Modelo** | Sonnet 5 |
| **Depende de** | F2.1 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-1, PL-2 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- CRUD (PL-1+PL-2) de **Cuenta bancaria / cuenta de dinero**: banco/entidad, alias, moneda (ARS/USD), tipo (CC, caja de ahorro, Mercado Pago, caja física, otra), estado de conciliación, activo. Ejemplos a precargar como seed: Banco Galicia CC (ARS), Banco Galicia USD, Mercado Pago.
- CRUD de **Tarjeta de crédito**: entidad, moneda, día de cierre, día de vencimiento, cuenta bancaria de débito asociada, activo.
- **Lógica de saldo inicial** (esto excede la plantilla): cada cuenta/tarjeta tiene saldo inicial + fecha del saldo, ambos editables. Si se modifican, el sistema recalcula la evolución posterior de la cuenta. Diseñar el recálculo como servicio invocable (lo usarán conciliación y flujo de caja).
- Tests: modificar saldo inicial y verificar recálculo de saldos posteriores.

### Entradas que deben adjuntarse a la sesión

- Molde F1.8.
- Diseño de F1.1 (evolución de saldos).

### Salida esperada (definición de terminado)

- Maestros operativos con saldo inicial funcional y recálculo probado.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Las cuentas bancarias/dinero/tarjetas tienen saldo inicial con fecha, editable, desde el cual se calcula la evolución.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
