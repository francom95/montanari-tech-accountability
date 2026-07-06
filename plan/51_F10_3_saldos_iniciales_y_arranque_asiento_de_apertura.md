# [F10.3] Saldos iniciales y arranque (asiento de apertura)

> ## ⚙️ EJECUTAR CON: 🟠 **Claude Sonnet 5**
>
> Paso de implementación estándar. Seguí la especificación y las plantillas al pie de la letra; no rediseñes lo ya decidido.

| Campo | Valor |
|---|---|
| **Paso** | 51 de 55 |
| **ID** | F10.3 |
| **Fase** | F10 — Migración desde Excel |
| **Modelo** | Sonnet 5 |
| **Depende de** | F10.2 |
| **Checkpoint humano** | Sí — el contador valida saldos de apertura contra el Excel y los resúmenes bancarios. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Cargar saldos iniciales de cuentas bancarias/dinero/tarjetas con su fecha (F2.4), tomados del Excel y de los resúmenes reales.
- Construir el **asiento de apertura contable**: saldos de apertura de todas las cuentas patrimoniales a la fecha de corte definida por el equipo, balanceado contra Patrimonio Neto / resultados acumulados según indique el contador.
- Ejecutar importación de facturación histórica (F4.6) y de resúmenes bancarios históricos (F5.2) hasta la fecha de corte, dejándolos conciliables.
- **Verificación de cuadratura**: el balance de sumas y saldos (F7.2) debe cerrar; los saldos bancarios del sistema deben coincidir con los resúmenes a la fecha de corte; documentar cualquier diferencia con su explicación.
- Checklist de arranque: qué queda pendiente de revisar en la bandeja (F5.1) para las primeras semanas de uso.

### Entradas que deben adjuntarse a la sesión

- Importadores F10.2 ejecutados.
- F4.6 y F5.2 operativos.
- Definición de fecha de corte del equipo.
- Datos del contador para el asiento de apertura.

### Salida esperada (definición de terminado)

- Sistema con punto de partida balanceado y validado por el contador.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el contador valida saldos de apertura contra el Excel y los resúmenes bancarios.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Las cuentas bancarias/dinero/tarjetas tienen saldo inicial con fecha, editable, desde el cual se calcula la evolución.
- Las importaciones bancarias entran como 'pendientes de revisar'; nunca crean movimientos definitivos automáticamente.
