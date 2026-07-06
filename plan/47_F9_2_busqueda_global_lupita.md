# [F9.2] Búsqueda global 'Lupita'

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 47 de 55 |
| **ID** | F9.2 |
| **Fase** | F9 — Transversales |
| **Modelo** | Sonnet 5 |
| **Depende de** | F8.5 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Búsqueda unificada sobre: asientos, facturas, clientes, proveedores, proyectos, etapas, cuentas contables, pagos, cobros, movimientos bancarios, tarjetas, vencimientos, pendientes administrativos, impuestos.
- Términos soportados: descripción, número, importe, fecha, CUIT, razón social, proyecto, cuenta, palabra clave. Detección del tipo de término (si parece CUIT buscar por CUIT; si parece importe, por importe con tolerancia; si parece fecha, por fecha).
- Implementación: índices FULLTEXT de MySQL para texto + búsqueda estructurada por tipo detectado; endpoint único que federa y devuelve resultados agrupados por tipo de entidad con límite por grupo.
- UI: barra global (atajo Ctrl/Cmd+K), resultados agrupados, clic lleva al registro con acciones según permisos (ver/editar/duplicar/anular/eliminar).
- Performance: responder < 500ms con datos migrados; paginación 'ver más' por grupo.

### Entradas que deben adjuntarse a la sesión

- Todas las entidades existentes.
- Permisos F1.5.

### Salida esperada (definición de terminado)

- Barra de búsqueda global operativa sobre las 14 entidades.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
