# [F6.3] Asociación de impuestos a proyectos

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 34 de 55 |
| **ID** | F6.3 |
| **Fase** | F6 — Impuestos |
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

- Permitir atribuir impuestos liquidados (IVA, IIBB) a proyectos: atribución directa (100% a un proyecto) o **prorrateo configurable** (por facturación del período, por margen, o porcentaje manual).
- El criterio de prorrateo es un parámetro del sistema, editable por admin, con posibilidad de override por liquidación.
- Persistir la atribución para que F7.4 (reporte por proyecto) la consuma sin recalcular.
- UI: dentro de la liquidación confirmada, sección 'atribución a proyectos' con la distribución y edición.
- Tests: prorrateo por facturación con 3 proyectos suma exactamente el total del impuesto (sin errores de redondeo: usar BigDecimal con estrategia de redondeo definida y ajustar el residuo en la última línea).

### Entradas que deben adjuntarse a la sesión

- Liquidaciones F6.1/F6.2.
- Proyectos F2.5.

### Salida esperada (definición de terminado)

- Impuestos atribuidos a proyectos, visibles para el reporte de rentabilidad.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
