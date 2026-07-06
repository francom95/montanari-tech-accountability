# [F5.2] Parsers de resúmenes (Galicia, Mercado Pago, tarjetas)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 29 de 55 |
| **ID** | F5.2 |
| **Fase** | F5 — Bancos y conciliación |
| **Modelo** | Sonnet 5 |
| **Depende de** | F5.1 |
| **Checkpoint humano** | Sí — verificar con un resumen real de cada origen. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Arquitectura de parsers por **estrategia** (interfaz `ResumenParser` + implementación por origen/formato) para poder sumar bancos sin tocar el core.
- Implementar: **Galicia** (Excel/CSV según lo que exporte el home banking), **Mercado Pago** (CSV/Excel de actividad), **resumen de tarjeta** (Excel/CSV; PDF solo texto básico con PDFBox).
- Cada parser normaliza a Movimiento bancario `pendiente de revisar` (F5.1): fecha, descripción, importe con signo, referencia.
- Manejo robusto de errores de archivos reales: encabezados variables, filas basura, separadores decimales AR (coma), fechas dd/mm/yyyy, encoding.
- Detección de duplicados al re-importar (hash de fecha+importe+descripción+cuenta).
- IMPORTANTE: pedir al equipo un resumen real de cada origen antes de codificar; si no están adjuntos, DETENETE y pedilos.

### Entradas que deben adjuntarse a la sesión

- Resúmenes reales de Galicia, MP y tarjeta (OBLIGATORIOS).
- Bandeja F5.1.

### Salida esperada (definición de terminado)

- Importación funcionando con los 3 orígenes contra archivos reales.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: verificar con un resumen real de cada origen.

### Reglas de negocio que NO se pueden romper en este paso

- Las importaciones bancarias entran como 'pendientes de revisar'; nunca crean movimientos definitivos automáticamente.
