# [F3.1] Diseño del motor contable

> ## ⚙️ EJECUTAR CON: 🟣 **Claude Fable 5**
>
> Paso crítico de diseño o revisión. Tomate el razonamiento que necesites: la salida de este paso condiciona todo el proyecto. Priorizá corrección sobre velocidad.

| Campo | Valor |
|---|---|
| **Paso** | 16 de 55 |
| **ID** | F3.1 |
| **Fase** | F3 — Núcleo contable |
| **Modelo** | Fable 5 |
| **Depende de** | F1.1 |
| **Checkpoint humano** | Sí — validación del contador sobre las reglas (especialmente diferencias de cambio y anulaciones). |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Especificar en detalle el núcleo contable, tomando el DER de F1.1 como base:
- **Plan de cuentas**: jerarquía cuentas madre (agrupan, no imputables) / cuentas imputables; código único, nombre, tipo, rubro, tipo de saldo esperado (deudor/acreedor), activo/inactivo, proyectos de uso habitual; creación de nuevas categorías/rubros/cuentas.
- **Asientos**: estructura multilínea con numeración interna automática compartida por todas las líneas de la operación; fecha, leyenda, cuentas, debe/haber, proyecto/cliente/proveedor/destino de fondos/origen opcionales por línea o cabecera (decidir y justificar); borradores; regla de balanceo.
- **Ciclo de vida**: edición de asientos automáticos post-generación, duplicación, anulación (decidir: contra-asiento vs marca de anulado, y cuándo cada una), permisos por rol.
- **Mayores y saldos**: cálculo de saldo acumulado y saldo final deudor/acreedor; interacción con períodos (asientos en períodos cerrados).
- **Multimoneda dentro del asiento**: cómo conviven importe original, TC y convertido ARS; cuándo y cómo se calcula y registra la **diferencia de cambio** (cobros/pagos USD contra registros ARS); cuentas de diferencia de cambio ganada/perdida.
- **Casos de prueba contables**: redactar al menos 15 casos concretos con números (venta USD cobrada en dos pagos parciales con TC distinto, anulación de factura confirmada, asiento con fecha intermedia, etc.) que servirán como tests de aceptación en F3.4, F4.x y F11.1.

### Entradas que deben adjuntarse a la sesión

- Salida de F1.1 (arquitectura y DER).
- Secciones 4, 5.6, 11 y 15 del documento fuente.

### Salida esperada (definición de terminado)

- Especificación técnica del motor contable + set de casos de prueba numéricos. Debe poder implementarse sin consultas adicionales.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: validación del contador sobre las reglas (especialmente diferencias de cambio y anulaciones).

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Se permiten asientos con fecha intermedia (sin orden cronológico de carga). El ordenamiento y los reportes se basan en la fecha del asiento.
- Los asientos automáticos son editables después de generados; toda edición queda en auditoría.
- Las cuentas madre solo agrupan: no son imputables. Solo cuentas imputables reciben movimientos.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
- El cierre de período evita modificaciones accidentales pero no bloquea consultas, importaciones ni exportaciones. Modificar un período cerrado requiere rol admin y auditoría reforzada.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.

### Nota para el operador

Ejecutar con Fable 5: es el corazón del sistema y sus decisiones (diferencias de cambio, anulaciones) son las más difíciles de corregir después.
