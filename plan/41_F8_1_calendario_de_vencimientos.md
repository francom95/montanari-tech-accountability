# [F8.1] Calendario de vencimientos

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 41 de 55 |
| **ID** | F8.1 |
| **Fase** | F8 — Presupuesto, vencimientos y caja |
| **Modelo** | Sonnet 5 |
| **Depende de** | F6.2 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Entidad **Vencimiento**: concepto, tipo (IVA, IIBB, Ganancias, Bienes Personales Acc. y Part., cargas sociales, sueldos, contador, tarjeta, suscripción, préstamo, plan de pago, pago automático, otro), fecha, importe estimado, moneda, recurrencia (única/mensual/anual/personalizada), asociaciones opcionales: cuenta contable, proveedor, impuesto/liquidación, tarjeta, proyecto, concepto recurrente.
- Estados: pendiente / pagado / vencido (automático al pasar la fecha) / reprogramado / cancelado.
- Generación automática de vencimientos desde: liquidaciones confirmadas de IVA/IIBB (F6), cierres de tarjeta (F5.4), conceptos recurrentes (F2.1).
- UI: vista calendario mensual + vista lista con filtros; marcar pagado vincula (opcionalmente) al pago/asiento real.
- Expone servicio de 'próximos vencimientos' para alertas (F9.1) y flujo proyectado (F8.3).

### Entradas que deben adjuntarse a la sesión

- Conceptos recurrentes F2.1.
- Liquidaciones F6.
- Tarjetas F5.4.

### Salida esperada (definición de terminado)

- Calendario operativo con estados y generación automática desde impuestos/tarjetas/recurrentes.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
