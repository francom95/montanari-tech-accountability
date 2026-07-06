# [F7.2] Balance de sumas y saldos

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 36 de 55 |
| **ID** | F7.2 |
| **Fase** | F7 — Reportes y dashboard |
| **Modelo** | Sonnet 5 |
| **Depende de** | F7.1 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-3 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Reporte con TODAS las cuentas: total debe, total haber, saldo (deudor/acreedor), agrupado por jerarquía del plan (madre suma sus hijas).
- **Verificación de balanceo global**: Σ debe = Σ haber del período; si no cierra, banner de error con la diferencia (es señal de bug: investigar, nunca ocultar).
- Drill-down: desde cada línea, abrir el mayor de la cuenta (F3.6) con los mismos filtros de período.
- Filtros: período, incluir/excluir cuentas sin movimiento, nivel de jerarquía.
- Exportable (F7.1).

### Entradas que deben adjuntarse a la sesión

- Mayores F3.6.
- Export F7.1.

### Salida esperada (definición de terminado)

- Balance exportable, con control de integridad y drill-down.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
