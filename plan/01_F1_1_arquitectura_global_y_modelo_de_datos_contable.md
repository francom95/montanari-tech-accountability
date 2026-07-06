# [F1.1] Arquitectura global y modelo de datos contable

> ## ⚙️ EJECUTAR CON: 🟣 **Claude Fable 5**
>
> Paso crítico de diseño o revisión. Tomate el razonamiento que necesites: la salida de este paso condiciona todo el proyecto. Priorizá corrección sobre velocidad.

| Campo | Valor |
|---|---|
| **Paso** | 1 de 55 |
| **ID** | F1.1 |
| **Fase** | F1 — Fundaciones |
| **Modelo** | Fable 5 |
| **Depende de** | — (sin dependencias) |
| **Checkpoint humano** | Sí — el equipo y el contador aprueban DER y arquitectura antes de escribir código. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Diseñar la arquitectura de capas del backend (controller/service/repository, manejo de errores, convenciones de paquetes y nombres).
- Producir el diagrama entidad-relación COMPLETO de los 10 módulos: maestros (clientes, proveedores, proyectos, etapas, presupuestos, comisionistas, cuentas bancarias/dinero/tarjetas, monedas, TC, jurisdicciones, categorías, rubros, costos), contabilidad (plan de cuentas jerárquico, asientos multilínea, períodos), facturación (facturas venta/compra, cobros, pagos, imputaciones), bancos (movimientos, conciliación, resúmenes de tarjeta), impuestos (liquidaciones IVA/IIBB), presupuesto/vencimientos/inversiones, pendientes administrativos, usuarios/roles, auditoría, alertas.
- Definir la estrategia multi-tenant-ready: columna `tenant_id` + filtro Hibernate, sin UI multiempresa en esta etapa.
- Diseñar el esquema de auditoría transversal: tabla de log con entidad, acción, usuario, fecha/hora, datos anteriores y nuevos en JSON.
- Diseñar el modelo de períodos contables mensuales y las reglas de cierre/reapertura (ver reglas abajo).
- Definir el tratamiento multimoneda a nivel de datos: en cada operación, moneda original, TC, fuente del TC, importe original, importe en ARS.
- Registrar cada decisión relevante como ADR corto.

### Entradas que deben adjuntarse a la sesión

- El brief completo del proyecto (documento adjunto).
- El documento funcional fuente (secciones 1 a 19).

### Salida esperada (definición de terminado)

- Documento de arquitectura + DER (formato mermaid o dbdiagram) + lista de ADRs. Debe ser suficiente para que otro modelo implemente sin volver a preguntar.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el equipo y el contador aprueban DER y arquitectura antes de escribir código.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Se permiten asientos con fecha intermedia (sin orden cronológico de carga). El ordenamiento y los reportes se basan en la fecha del asiento.
- Las cuentas madre solo agrupan: no son imputables. Solo cuentas imputables reciben movimientos.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Las cuentas bancarias/dinero/tarjetas tienen saldo inicial con fecha, editable, desde el cual se calcula la evolución.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
- El cierre de período evita modificaciones accidentales pero no bloquea consultas, importaciones ni exportaciones. Modificar un período cerrado requiere rol admin y auditoría reforzada.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
- Toda entidad de negocio incluye `tenant_id` y filtro correspondiente, aunque la UI no muestre multiempresa.

### Nota para el operador

Este es el paso de mayor apalancamiento del proyecto: por eso se ejecuta con Fable 5 y no con Opus. No implementes código; solo diseño.
