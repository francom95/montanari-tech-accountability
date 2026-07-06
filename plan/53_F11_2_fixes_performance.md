# [F11.2] Fixes + performance

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 53 de 55 |
| **ID** | F11.2 |
| **Fase** | F11 — Hardening y revisión final |
| **Modelo** | Sonnet 5 |
| **Depende de** | F11.1 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Resolver TODOS los hallazgos bloqueantes y altos del informe F11.1 (adjuntarlo completo); medios/bajos según decisión del equipo.
- Performance con datos reales migrados: índices MySQL para mayores, balance, reportes, CxC/CxP y Lupita (EXPLAIN de las queries principales); paginación donde falte; tiempos objetivo: reportes < 3s, Lupita < 500ms, dashboard < 2s.
- Re-ejecutar la suite completa de tests + los casos contables de F3.1/F4.1/F6.x tras cada fix.
- Entregar changelog de fixes con referencia al hallazgo.

### Entradas que deben adjuntarse a la sesión

- Informe F11.1.
- Base con datos migrados.

### Salida esperada (definición de terminado)

- Hallazgos cerrados, tiempos objetivo cumplidos, suite verde.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
