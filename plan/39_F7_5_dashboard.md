# [F7.5] Dashboard

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 39 de 55 |
| **ID** | F7.5 |
| **Fase** | F7 — Reportes y dashboard |
| **Modelo** | Sonnet 5 |
| **Depende de** | F7.4 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Pantalla de inicio con estos indicadores del período seleccionado: ventas; cobros realizados; cuentas por cobrar; cuentas por pagar; saldo de caja; saldo por banco/cuenta; impuestos próximos a vencer; resultado mensual; margen estimado; egresos proyectados; obligaciones próximas; zona de alertas importantes (se conecta con F9.1: dejar el slot listo).
- Cada widget con drill-down a su reporte/pantalla de origen.
- Selector de período global del dashboard.
- Performance: endpoints agregados específicos (no N llamadas), cacheo corto si hace falta.
- Diseño limpio con la biblioteca UI elegida; placeholder de logo en header.

### Entradas que deben adjuntarse a la sesión

- Reportes F7.2-F7.4.
- CxC/CxP F4.5.
- Saldos F2.4/F5.3.

### Salida esperada (definición de terminado)

- Dashboard como home del sistema con 12 indicadores y drill-down.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
