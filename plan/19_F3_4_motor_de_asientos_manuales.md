# [F3.4] Motor de asientos manuales

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 19 de 55 |
| **ID** | F3.4 |
| **Fase** | F3 — Núcleo contable |
| **Modelo** | Sonnet 5 |
| **Depende de** | F3.2 |
| **Checkpoint humano** | No |
| **Plantillas usadas** | PL-5 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Implementar según spec F3.1: creación de asientos multilínea con numeración interna automática compartida.
- Fechas intermedias sin restricción cronológica (cargar 16/06 existiendo 10/06 y 20/06).
- Balanceo obligatorio para confirmar; si no balancea solo se guarda como borrador (PL-5).
- Asociación opcional a proyecto, cliente, proveedor, destino de fondos, origen, observaciones.
- Multimoneda por línea u operación según lo decidido en F3.1: moneda original + TC + convertido ARS.
- UI de carga ágil: grilla de líneas con autocompletar de cuentas imputables (las madre no aparecen), totales debe/haber en vivo, diferencia resaltada.
- Implementar TODOS los casos de prueba de F3.1 que apliquen a asientos manuales como tests automatizados.

### Entradas que deben adjuntarse a la sesión

- Spec F3.1 con casos de prueba.
- Plan de cuentas F3.2/F3.3.

### Salida esperada (definición de terminado)

- Asientos manuales end-to-end con los casos de F3.1 en verde.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Se permiten asientos con fecha intermedia (sin orden cronológico de carga). El ordenamiento y los reportes se basan en la fecha del asiento.
- Las cuentas madre solo agrupan: no son imputables. Solo cuentas imputables reciben movimientos.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
