# [F4.1] Reglas de asientos automáticos e imputación de cobros/pagos

> ## ⚙️ EJECUTAR CON: 🔴 **Claude Opus 4.8**
>
> Paso de razonamiento pesado (lógica contable/fiscal sensible). Priorizá corrección y casos borde; incluí casos de prueba en la salida.

| Campo | Valor |
|---|---|
| **Paso** | 22 de 55 |
| **ID** | F4.1 |
| **Fase** | F4 — Facturación, cobros y pagos |
| **Modelo** | Opus 4.8 |
| **Depende de** | F3.1 |
| **Checkpoint humano** | Sí — el contador valida cada regla de asiento con los casos numéricos. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Especificar los `AsientoGenerator` (PL-4) para cada evento, con el detalle línea por línea (cuenta, debe/haber, cómo se calcula el importe):
- **Factura de venta confirmada**: Créditos por Ventas (CxC) al debe por el total; IVA Débito Fiscal al haber; Ingresos por Ventas u Otros Ingresos por Ventas al haber según corresponda; tratamiento de percepciones/retenciones sufridas en la factura.
- **Factura de compra confirmada**: costo o gasto según categoría contable al debe; IVA Crédito Fiscal al debe si corresponde (según condición IVA del proveedor y tipo de comprobante); percepciones al debe; Deudas Comerciales (CxP) al haber.
- **Cobro** (total, parcial, anticipo, a cuenta): banco/caja/MP al debe; CxC al haber; por medio de pago y moneda; si moneda ≠ ARS, cálculo de **diferencia de cambio** entre TC de factura y TC de cobro (cuentas de diferencia de cambio ganada/perdida según F3.1); retenciones sufridas en el cobro.
- **Pago** (total, parcial, anticipo): CxP al debe; banco/caja al haber; diferencia de cambio; retenciones practicadas si aplica.
- **Mapeo configurable de cuentas**: tabla de parametrización concepto→cuenta contable (por tipo de operación, categoría, medio de pago) editable por el admin, para no hardcodear el plan de cuentas en el código.
- **Regla de edición**: el asiento generado es editable después (agregar cuentas sin alterar el comprobante origen, para ajustes/reclasificaciones).
- Redactar casos de prueba numéricos por cada generator (mínimo 3 por evento, incluyendo parciales y USD), coherentes con los casos de F3.1.

### Entradas que deben adjuntarse a la sesión

- Spec del motor contable F3.1 (adjuntar completa).
- Secciones 5 y 11 del documento fuente.

### Salida esperada (definición de terminado)

- Especificación de reglas por evento + esquema de la tabla de mapeo de cuentas + casos de prueba numéricos.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el contador valida cada regla de asiento con los casos numéricos.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Los asientos automáticos son editables después de generados; toda edición queda en auditoría.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.

### Nota para el operador

No implementes: especificá. Sonnet implementará en F4.2-F4.4 exactamente lo que escribas acá.
