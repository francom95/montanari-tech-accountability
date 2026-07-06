# [F4.6] Importación de facturación histórica

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 27 de 55 |
| **ID** | F4.6 |
| **Fase** | F4 — Facturación, cobros y pagos |
| **Modelo** | Sonnet 5 |
| **Depende de** | F4.3 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Importador de facturas históricas (ventas y compras) desde **Excel/CSV** con Apache POI/OpenCSV: mapeo de columnas configurable, previsualización, validación fila por fila (cliente/proveedor existente o alta rápida, CUIT, importes, moneda/TC), reporte de rechazos descargable.
- Elección al importar: cargar como `confirmado` (genera asientos) o como `borrador` (revisión posterior).
- **PDF**: solo extracción básica de texto con PDFBox + formulario de carga asistida pre-completado; el parsing avanzado de PDF queda explícitamente para la etapa 2.
- Idempotencia: re-importar el mismo archivo no duplica (clave: tipo+punto de venta+número).
- Tests con archivos de ejemplo reales.

### Entradas que deben adjuntarse a la sesión

- Facturas F4.2/F4.3.
- Archivos de ejemplo reales del equipo (pedirlos si no están adjuntos).

### Salida esperada (definición de terminado)

- Facturas históricas importadas, con rechazos claros, listas para cruzar con bancos en F5.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
