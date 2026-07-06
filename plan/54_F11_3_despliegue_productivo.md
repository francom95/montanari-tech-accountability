# [F11.3] Despliegue productivo

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 54 de 55 |
| **ID** | F11.3 |
| **Fase** | F11 — Hardening y revisión final |
| **Modelo** | Sonnet 5 |
| **Depende de** | F11.2 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Dockerfiles de producción optimizados (multi-stage, JRE mínima, build React estático).
- Nginx: sirve el build de React, proxy `/api` al backend, HTTPS (certbot o certificado provisto), headers de seguridad básicos.
- **Backups automáticos de MySQL**: dump diario + retención configurable + copia fuera del servidor; **procedimiento de restore probado de punta a punta** (esto es innegociable en un sistema contable).
- Perfil prod: secretos por variables de entorno, logs con rotación, Actuator restringido.
- Runbook: cómo desplegar, cómo restaurar, cómo rotar secretos.

### Entradas que deben adjuntarse a la sesión

- Sistema post-fixes F11.2.
- Servidor/hosting definido por el equipo (preguntar si no está definido).

### Salida esperada (definición de terminado)

- Entorno productivo con backup/restore verificado y runbook.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
