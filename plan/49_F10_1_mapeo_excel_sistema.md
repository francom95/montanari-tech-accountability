# [F10.1] Mapeo Excel → sistema

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 49 de 55 |
| **ID** | F10.1 |
| **Fase** | F10 — Migración desde Excel |
| **Modelo** | Sonnet 5 |
| **Depende de** | F2.7 |
| **Checkpoint humano** | Sí — el equipo aprueba qué migra como dato, qué define estructura y qué queda afuera. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Analizar el Excel real completo (insumo OBLIGATORIO: si no está adjunto, DETENETE y pedilo) hoja por hoja: clientes; proyectos; etapas; proveedores de servicios; flujo de caja proyectado; flujo de caja detallado mensual; comisiones por ventas; calendario de vencimientos; inversiones en Fondos Fima; presupuesto de pagos; presupuestos estimados de proyectos; libro diario; estado de resultados; plan de cuentas; mayores; estado de situación patrimonial; base de datos de clientes; IVA a pagar; IIBB a pagar; pendientes administrativos.
- Para cada hoja producir: entidad(es) destino, mapeo columna→campo, transformaciones necesarias (formatos de fecha, monedas, textos libres a FKs), y la clasificación: **migra como dato definitivo** / **solo define estructura o fórmulas** / **entra como saldo inicial** / **no migra (y por qué)**.
- Detectar inconsistencias del Excel (duplicados, referencias rotas, totales que no cuadran) y listarlas para decisión del equipo.
- Priorizar el orden de importación por dependencias (clientes antes que proyectos, etc.).

### Entradas que deben adjuntarse a la sesión

- Excel completo real (OBLIGATORIO).
- Modelo de datos implementado (F2.x, F3.x).

### Salida esperada (definición de terminado)

- Documento de mapeo hoja por hoja con clasificación y orden de importación, listo para aprobar.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el equipo aprueba qué migra como dato, qué define estructura y qué queda afuera.

### Nota para el operador

Análisis, no código. La salida es el insumo directo de F10.2.
