# [F2.7] Comisionistas

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 15 de 55 |
| **ID** | F2.7 |
| **Fase** | F2 — Maestros |
| **Modelo** | Sonnet 5 |
| **Depende de** | F2.5 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-1, PL-2 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Entidad **Comisionista** (PL-1+PL-2) + vínculo N:M con proyectos.
- Por vínculo proyecto-comisionista: porcentaje de comisión, base de cálculo (configurable: monto total, monto cobrado, monto sin impuestos, etc.), moneda, importe estimado (calculado), importe final, estado de pago, fecha estimada de pago, observaciones.
- El cálculo del importe estimado se actualiza al cambiar el proyecto o la base.
- Dejar expuesto un servicio que devuelva comisiones devengadas/pendientes por proyecto y por período (lo consumirán CxP en F4, rentabilidad en F7.4 y flujo proyectado en F8.3).

### Entradas que deben adjuntarse a la sesión

- Molde F1.8.
- Proyectos F2.5.

### Salida esperada (definición de terminado)

- Gestión de comisionistas con cálculo automático y servicio de consulta expuesto.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
