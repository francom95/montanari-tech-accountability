# [F6.2] Lógica de IIBB (liquidación multi-jurisdicción)

> ## ⚙️ EJECUTAR CON: 🔴 **Claude Opus 4.8**
>
> Paso de razonamiento pesado (lógica contable/fiscal sensible). Priorizá corrección y casos borde; incluí casos de prueba en la salida.

| Campo | Valor |
|---|---|
| **Paso** | 33 de 55 |
| **ID** | F6.2 |
| **Fase** | F6 — Impuestos |
| **Modelo** | Opus 4.8 |
| **Depende de** | F6.1 |
| **Checkpoint humano** | Sí — validación del contador con una liquidación real. |
| **Plantillas usadas** | PL-4 (ver `00_plantillas.md`) |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Especificar E implementar la liquidación mensual de IIBB por jurisdicción (Buenos Aires, CABA, Córdoba, extensible vía maestro F2.1):
- Por jurisdicción: actividad y códigos, alícuotas configurables, **base imponible** (ventas atribuibles según jurisdicción de destino de la factura), impuesto determinado.
- Deducciones: **retenciones y percepciones** sufridas, **SIRCREB** (identificado en conciliación F5.3), **pagos a cuenta**, **saldos a favor anteriores** por jurisdicción (arrastre).
- Pantalla editable antes de confirmar (misma UX y componente base que F6.1); asiento automático al confirmar contra pasivo fiscal o saldo a favor por jurisdicción.
- Reutilizar la infraestructura de liquidación de F6.1 (misma sesión de trabajo recomendada para amortizar contexto).
- Tests: liquidación de un mes real por al menos 2 jurisdicciones validada con el contador.

### Entradas que deben adjuntarse a la sesión

- Infraestructura de liquidación F6.1.
- Jurisdicciones F2.1.
- Sección 8.3 del documento fuente.
- Liquidación real de ejemplo (pedirla).

### Salida esperada (definición de terminado)

- Liquidación de IIBB multi-jurisdicción editable + asientos + tests validados.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: validación del contador con una liquidación real.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Los asientos automáticos son editables después de generados; toda edición queda en auditoría.
- Estados mínimos: borrador / confirmado / anulado. Solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.

### Nota para el operador

Ejecutar en la MISMA sesión que F6.1 si es posible: comparten infraestructura y contexto (ahorro de tokens).
