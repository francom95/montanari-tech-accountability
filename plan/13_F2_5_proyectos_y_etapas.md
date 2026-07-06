# [F2.5] Proyectos y etapas

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 13 de 55 |
| **ID** | F2.5 |
| **Fase** | F2 — Maestros |
| **Modelo** | Sonnet 5 |
| **Depende de** | F2.2, F2.3 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-1, PL-2 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Entidad **Proyecto**: nombre, cliente (FK), responsable, país, tipo de proyecto, estado, moneda, monto total, cantidad de pagos pactados, cuotas (fecha estimada de cobro + importe por cuota, 1:N), comentarios, estado comercial, estado de facturación, estado de cobranza, fecha estimada de finalización, fecha real de finalización.
- Entidad **Etapa** (1:N con proyecto, varias en curso simultáneas): nombre, descripción, estado, fecha inicio, fecha estimada de fin, % de avance, monto presupuestado, costos estimados, proveedores asociados (N:M), pagos previstos, cobros previstos, observaciones.
- Carga de etapas: manual y por importación desde archivo (Excel/CSV con POI/OpenCSV, previsualización antes de confirmar).
- UI: ficha de proyecto con pestañas (datos, cuotas, etapas, y placeholders para presupuesto F2.6 y reporte F7.4).
- Restricción: no eliminar proyecto con facturas/cobros/pagos asociados.

### Entradas que deben adjuntarse a la sesión

- Molde F1.8.
- Clientes F2.2 y Proveedores F2.3.

### Salida esperada (definición de terminado)

- Gestión completa de proyectos con etapas e importación de etapas funcionando.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
- Toda entidad de negocio incluye `tenant_id` y filtro correspondiente, aunque la UI no muestre multiempresa.
