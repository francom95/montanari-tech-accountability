# [F1.4] Scaffolding frontend

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 4 de 55 |
| **ID** | F1.4 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Sonnet 5 |
| **Depende de** | F1.2 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Proyecto React 18 + TypeScript + Vite.
- React Router con layout base: sidebar con los módulos del sistema, header con placeholder de logo (el logo llegará luego), zona de contenido.
- React Query configurado (client, devtools en dev).
- React Hook Form + Zod instalados con un ejemplo de formulario tipado.
- Wrapper HTTP tipado (Axios o fetch) con manejo de JWT (inyección de token, refresh, redirección a login en 401).
- Instalar y configurar la biblioteca UI decidida en F1.2.

### Entradas que deben adjuntarse a la sesión

- ADRs de F1.2.

### Salida esperada (definición de terminado)

- App corriendo con layout navegable vacío y build de producción funcionando.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
