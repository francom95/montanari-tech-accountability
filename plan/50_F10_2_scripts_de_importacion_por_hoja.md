# [F10.2] Scripts de importación por hoja

> ## ⚙️ EJECUTAR CON: 🟢 **Claude Haiku 4.5**
>
> Paso de volumen sobre plantilla. NO tomes decisiones de diseño: replicá el molde de referencia (F1.8) exactamente, solo cambiando entidad y campos. Si algo no encaja en la plantilla, detenete y reportalo en lugar de improvisar.

| Campo | Valor |
|---|---|
| **Paso** | 50 de 55 |
| **ID** | F10.2 |
| **Fase** | F10 — Migración desde Excel |
| **Modelo** | Haiku 4.5 |
| **Depende de** | F10.1 |
| **Checkpoint humano** | No |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Implementar un importador por hoja aprobada en F10.1, siguiendo TODOS el mismo patrón (definirlo con el primero y replicar): leer archivo (POI/OpenCSV) → validar fila por fila según el mapeo → resolver FKs → insertar vía services existentes (NUNCA SQL directo, para que corran validaciones y auditoría) → reporte de rechazos con motivo por fila.
- Idempotencia: re-ejecutar no duplica (clave natural por entidad definida en el mapeo F10.1).
- Modo dry-run: validar todo sin insertar y emitir el reporte.
- Hojas típicas: clientes, proveedores, proyectos, etapas, comisiones, vencimientos, inversiones, pendientes administrativos, conceptos recurrentes.
- NO improvises transformaciones que no estén en el mapeo de F10.1: si una fila no encaja, va al reporte de rechazos.

### Entradas que deben adjuntarse a la sesión

- Documento de mapeo F10.1 (adjuntar completo).
- Services existentes de cada entidad.

### Salida esperada (definición de terminado)

- Importadores ejecutables, idempotentes, con dry-run y reporte de rechazos.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Nota para el operador

Patrón repetitivo: el primero define el molde, el resto lo copia.
