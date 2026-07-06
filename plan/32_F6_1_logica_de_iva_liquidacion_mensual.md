# [F6.1] Lógica de IVA (liquidación mensual)

> ## ⚙️ EJECUTAR CON: 🔴 **Claude Opus 4.8**
>
> Paso de razonamiento pesado (lógica contable/fiscal sensible). Priorizá corrección y casos borde; incluí casos de prueba en la salida.

| Campo | Valor |
|---|---|
| **Paso** | 32 de 55 |
| **ID** | F6.1 |
| **Fase** | F6 — Impuestos |
| **Modelo** | Opus 4.8 |
| **Depende de** | F4.4, F5.3 |
| **Checkpoint humano** | Sí — el contador valida contra una liquidación real de un mes. |
| **Plantillas usadas** | PL-4 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Especificar E implementar la liquidación mensual de IVA:
- **Débito fiscal**: desde facturas de venta confirmadas del período.
- **Crédito fiscal**: desde facturas de compra confirmadas + comisiones bancarias con IVA identificadas en conciliación (F5.3) + otros asientos marcados como con crédito fiscal.
- **Percepciones de IVA** sufridas (facturas y bancos), **restituciones**, **saldo técnico del período anterior** (arrastre automático), **saldo de libre disponibilidad** si aplica.
- Resultado: saldo técnico, saldo a favor o saldo a pagar.
- **Pantalla de liquidación editable ANTES de confirmar**: cada componente ajustable manualmente (con motivo), agregando o corrigiendo conceptos; los ajustes quedan auditados.
- **Al confirmar**: asiento automático (PL-4) contra Deudas Fiscales (a pagar) o crédito fiscal a favor según resultado; el período de IVA queda marcado como liquidado.
- Reversión: des-confirmar una liquidación (solo admin) anula el asiento según regla F3.1.
- Tests con los casos numéricos que valides con el contador: mes con saldo a pagar, mes con saldo a favor arrastrado, mes con percepciones.

### Entradas que deben adjuntarse a la sesión

- Facturas F4.2/F4.3, cobros/pagos F4.4, conciliación F5.3.
- Sección 8.2 del documento fuente.
- Una liquidación real de ejemplo del contador (pedirla).

### Salida esperada (definición de terminado)

- Liquidación de IVA editable + asiento al confirmar + tests de los casos del contador en verde.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el contador valida contra una liquidación real de un mes.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Los asientos automáticos son editables después de generados; toda edición queda en auditoría.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
- El cierre de período evita modificaciones accidentales pero no bloquea consultas, importaciones ni exportaciones. Modificar un período cerrado requiere rol admin y auditoría reforzada.

### Nota para el operador

Lógica fiscal argentina sensible: ante ambigüedad normativa, dejá el punto como parámetro configurable y documentalo, no lo resuelvas por tu cuenta.
