# [F4.4] Cobros y pagos

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 25 de 55 |
| **ID** | F4.4 |
| **Fase** | F4 — Facturación, cobros y pagos |
| **Modelo** | Sonnet 5 |
| **Depende de** | F4.2, F4.3 |
| **Checkpoint humano** | Sí — el contador valida un caso real de cobro en USD con diferencia de cambio. |
| **Plantillas usadas** | PL-4, PL-5 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Registrar **cobros**: totales, parciales, anticipos y a cuenta; imputación contra una o varias facturas; medio: transferencia, Mercado Pago, caja, cuenta USD, otros; actualización de saldo pendiente y estado de la factura.
- Registrar **pagos** a proveedores: mismos casos, imputación contra facturas de compra.
- Multimoneda: si la moneda ≠ ARS, TC obligatorio; registrar moneda original + TC + convertido ARS; calcular y asentar **diferencia de cambio** según F4.1 (TC de factura vs TC de cobro/pago).
- Asientos automáticos de cobro y pago vía PL-4.
- Anticipos sin factura: cuenta de anticipos según F4.1, aplicables luego contra facturas.
- Tests obligatorios: cobro USD en dos parciales con TC distinto (caso de F3.1/F4.1), pago parcial ARS, anticipo aplicado.

### Entradas que deben adjuntarse a la sesión

- Spec F4.1.
- Facturas F4.2/F4.3.
- Cuentas F2.4.

### Salida esperada (definición de terminado)

- Circuito factura→cobro/pago completo con saldos correctos en ambas monedas y diferencias de cambio asentadas.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el contador valida un caso real de cobro en USD con diferencia de cambio.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
