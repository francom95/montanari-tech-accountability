# [F7.4] Reporte detallado por proyecto (rentabilidad)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 38 de 55 |
| **ID** | F7.4 |
| **Fase** | F7 — Reportes y dashboard |
| **Modelo** | Sonnet 5 |
| **Depende de** | F7.3, F6.3 |
| **Checkpoint humano** | Sí — validar con un proyecto real cerrado que el margen coincida con el Excel. |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- EL reporte clave del sistema. Por proyecto seleccionado mostrar TODO lo siguiente:
- Identificación: nombre, cliente, etapas con su avance, tiempo del proyecto, fecha inicio, fecha estimada de fin, fecha real de fin, estado.
- Ingresos: total presupuestado, total facturado, total cobrado, pendiente de cobro, cuotas cobradas vs pendientes.
- Egresos: proveedores del proyecto, presupuestado a pagar, pagado, pendiente de pago, cuotas pagadas vs pendientes.
- Comisiones: comisionista asociado, comisión calculada (F2.7), estado.
- Impuestos asociados (F6.3), costos y gastos imputados.
- Resultado: margen estimado (del presupuesto F2.6), margen real, **diferencia presupuesto vs real línea por línea**, rentabilidad final.
- Fuente de datos: cruzar el presupuesto estructurado de F2.6 con la ejecución real (F4.4, F2.7, F6.3). NO recalcular: consumir los servicios existentes.
- Exportable (F7.1) con formato apto para enviar al cliente interno.

### Entradas que deben adjuntarse a la sesión

- Presupuestos F2.6.
- Cobros/pagos F4.4.
- Comisiones F2.7.
- Impuestos F6.3.
- Export F7.1.

### Salida esperada (definición de terminado)

- Reporte por proyecto con comparativo proyectado/real y rentabilidad final, validado contra un caso real.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: validar con un proyecto real cerrado que el margen coincida con el Excel.

### Reglas de negocio que NO se pueden romper en este paso

- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
