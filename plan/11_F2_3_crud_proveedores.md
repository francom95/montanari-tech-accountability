# [F2.3] CRUD Proveedores

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 11 de 55 |
| **ID** | F2.3 |
| **Fase** | F2 — Maestros |
| **Modelo** | Haiku 4.5 |
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

- Aplicar PL-1 + PL-2 a **Proveedor** con campos: nombre, razón social, CUIT (misma validación que clientes), domicilio, datos de facturación, condición frente al IVA, email, teléfono, tipo de proveedor, servicios que presta, moneda habitual de pago (FK), observaciones, activo.
- Clasificación por tipo de servicio/costo: N:M con Tipo de costo (F2.1), permitiendo asociar a un costo existente o crear uno nuevo desde el mismo formulario.
- Restricción de eliminación por facturas/pagos asociados.

### Entradas que deben adjuntarse a la sesión

- Molde F1.8 + 00_plantillas.md.
- Tipos de costo de F2.1.

### Salida esperada (definición de terminado)

- CRUD completo con clasificación por costos y tests.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
