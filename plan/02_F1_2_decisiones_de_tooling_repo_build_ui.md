# [F1.2] Decisiones de tooling (repo, build, UI)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 2 de 55 |
| **ID** | F1.2 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Sonnet 5 |
| **Depende de** | F1.1 |
| **Checkpoint humano** | Sí — confirmación rápida del equipo. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Decidir monorepo vs multi-repo (sugerencia del equipo: monorepo con `/backend` y `/frontend`). Justificar en 5 líneas.
- Decidir Maven vs Gradle y dejarlo fijo para todo el proyecto.
- Decidir biblioteca de UI: shadcn/ui + Tailwind vs MUI. Considerar: densidad de tablas contables, formularios largos, velocidad de desarrollo con Haiku replicando pantallas.
- Escribir 3 ADRs cortos (uno por decisión).

### Entradas que deben adjuntarse a la sesión

- Salida de F1.1 (documento de arquitectura).

### Salida esperada (definición de terminado)

- 3 ADRs en markdown, cada uno con: contexto, opciones, decisión, consecuencias.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: confirmación rápida del equipo.

### Nota para el operador

No reabras decisiones ya tomadas en el stack (sección Contexto).
