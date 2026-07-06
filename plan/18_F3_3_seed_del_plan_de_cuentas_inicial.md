# [F3.3] Seed del plan de cuentas inicial

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 18 de 55 |
| **ID** | F3.3 |
| **Fase** | F3 — Núcleo contable |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F3.2 |
| **Checkpoint humano** | Sí — el contador revisa el plan cargado. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Transcribir el plan de cuentas del Excel actual a una migración Flyway de datos (seed).
- Incluir como mínimo las categorías Activo, Pasivo, Patrimonio Neto, Resultado Positivo, Resultado Negativo y los rubros: Caja y Bancos; Créditos por Ventas; Otros Créditos; Inversiones Transitorias; Deudas Comerciales; Deudas Sociales; Deudas Fiscales; Deudas Bancarias; Otras Deudas; Ingresos por Ventas; Otros Ingresos por Ventas; Costos de Prestación de Servicios; Gastos de Comercialización; Gastos de Administración; Gastos Financieros; Impuestos; Comisiones; Suscripciones; Intereses.
- Respetar códigos, jerarquía madre/imputable y tipo de saldo esperado según la hoja del Excel.
- No inventes cuentas: si algo del Excel es ambiguo, listalo como pregunta al final en lugar de decidir.

### Entradas que deben adjuntarse a la sesión

- Hoja 'plan de cuentas' del Excel (OBLIGATORIA).
- Plan de cuentas implementado F3.2.

### Salida esperada (definición de terminado)

- Migración Flyway con el plan inicial cargado y verificable en la UI de árbol.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el contador revisa el plan cargado.

### Reglas de negocio que NO se pueden romper en este paso

- Las cuentas madre solo agrupan: no son imputables. Solo cuentas imputables reciben movimientos.
