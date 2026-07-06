# [F4.2] Facturas de venta

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 23 de 55 |
| **ID** | F4.2 |
| **Fase** | F4 — Facturación, cobros y pagos |
| **Modelo** | Sonnet 5 |
| **Depende de** | F4.1 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-4, PL-5 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Carga de factura de venta con: fecha, cliente, proyecto asociado, número de comprobante, tipo de factura (A/B/C/E), jurisdicción de destino, moneda, TC, detalle de servicios, base imponible, alícuota de IVA, IVA calculado, percepciones/retenciones, importe total, estado de facturación, estado de cobro.
- PL-5 de estados: borrador / confirmado / anulado. Solo al confirmar se genera el asiento.
- Generación automática del asiento vía PL-4 según spec F4.1 (CxC / IVA débito / ingresos, percepciones).
- El asiento generado es editable post-generación: se pueden agregar cuentas SIN alterar el comprobante original (ajustes, reclasificaciones). Vínculo bidireccional factura↔asiento visible en ambas pantallas.
- Adjunto PDF del comprobante: opcional, nunca obligatorio.
- Anulación de factura: aplicar la regla de F3.1/F4.1 sobre el asiento vinculado.
- Tests: los casos de F4.1 para ventas, en verde.

### Entradas que deben adjuntarse a la sesión

- Spec F4.1.
- Motor de asientos F3.4/F3.5.
- Clientes F2.2, Proyectos F2.5.

### Salida esperada (definición de terminado)

- Factura de venta → asiento automático correcto, editable y auditado.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Los asientos automáticos son editables después de generados; toda edición queda en auditoría.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
