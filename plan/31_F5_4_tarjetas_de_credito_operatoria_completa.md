# [F5.4] Tarjetas de crédito (operatoria completa)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 31 de 55 |
| **ID** | F5.4 |
| **Fase** | F5 — Bancos y conciliación |
| **Modelo** | Sonnet 5 |
| **Depende de** | F5.3 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-4 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Ciclo del resumen de tarjeta: importar (F5.2) → clasificar consumos → pagar → conciliar.
- Clasificación de consumos: cuenta contable + asociación opcional a proveedor, proyecto, suscripción (concepto recurrente), impuesto u otros gastos. Clasificación masiva por reglas simples (si descripción contiene X → cuenta Y).
- Impuestos e intereses del resumen: líneas identificables con imputación a sus cuentas.
- **Pago del resumen**: genera o vincula asiento (PL-4) contra la cuenta bancaria de débito (F2.4); saldo pendiente si el pago es parcial (pago mínimo).
- Conciliación del pago contra el movimiento bancario correspondiente (F5.3).
- Vista por tarjeta: cierre, vencimiento, resumen actual, consumos clasificados/sin clasificar, saldo.

### Entradas que deben adjuntarse a la sesión

- Tarjetas F2.4.
- Parsers F5.2.
- Reglas F4.1.

### Salida esperada (definición de terminado)

- Ciclo completo de tarjeta funcionando de importación a conciliación.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Las importaciones bancarias entran como 'pendientes de revisar'; nunca crean movimientos definitivos automáticamente.
- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
