# [F2.6] Presupuesto estimado por proyecto

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 14 de 55 |
| **ID** | F2.6 |
| **Fase** | F2 — Maestros |
| **Modelo** | Sonnet 5 |
| **Depende de** | F2.5, F10.1 (mapeo de la hoja de fórmulas; puede adelantarse solo esa hoja) |
| **Checkpoint humano** | Sí — el contador/equipo valida que los cálculos repliquen el Excel de referencia. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Implementar el motor de presupuesto estimado por proyecto basado en la **hoja de fórmulas de referencia del Excel** (insumo obligatorio: si no está adjunta, DETENETE y pedila).
- El presupuesto calcula: costos, márgenes, impuestos estimados, comisiones, rentabilidad estimada, cuotas de cobro, pagos a proveedores, resultado esperado del proyecto.
- Las fórmulas/parámetros deben ser configurables como maestro (no hardcodear alícuotas ni porcentajes).
- Diseñar la estructura de datos para la comparación proyectado vs real que consumirá F7.4: cada línea presupuestada debe poder cruzarse con su ejecución real (cobros, pagos, impuestos, comisiones).
- UI: editor de presupuesto dentro de la ficha del proyecto, con totales y márgenes recalculados en vivo.
- Tests: replicar 2-3 presupuestos reales del Excel y verificar que los números coinciden.

### Entradas que deben adjuntarse a la sesión

- Hoja de fórmulas del Excel (OBLIGATORIA).
- Proyectos F2.5.
- Mapeo parcial de F10.1 si existe.

### Salida esperada (definición de terminado)

- Alta/edición de presupuesto con cálculos automáticos que replican el Excel, listo para comparativo real.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el contador/equipo valida que los cálculos repliquen el Excel de referencia.

### Reglas de negocio que NO se pueden romper en este paso

- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
