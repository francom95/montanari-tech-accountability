# [F11.1] Revisión final de seguridad e integridad contable

> ## ⚙️ EJECUTAR CON: 🟣 **Claude Fable 5**
>
> Paso crítico de diseño o revisión. Tomate el razonamiento que necesites: la salida de este paso condiciona todo el proyecto. Priorizá corrección sobre velocidad.

| Campo | Valor |
|---|---|
| **Paso** | 52 de 55 |
| **ID** | F11.1 |
| **Fase** | F11 — Hardening y revisión final |
| **Modelo** | Fable 5 |
| **Depende de** | F10.3 |
| **Checkpoint humano** | Sí — el equipo decide qué hallazgos bloquean el go-live. |

---

## Contexto del proyecto (incluir siempre en la sesión)

**Proyecto:** Sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech (reemplaza el Excel actual). Contabilidad de gestión interna, fiscalidad **argentina** (IVA, IIBB, SIRCREB, percepciones/retenciones). Multimoneda **ARS/USD** con tipo de cambio configurable y diferencias de cambio. No emite facturas ante ARCA/AFIP: registra comprobantes ya emitidos/recibidos.

**Stack fijado:** Backend Java 21 + Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator), Hibernate, Flyway, MapStruct, Lombok, springdoc-openapi. BD MySQL 8 (importes siempre `DECIMAL`/`BigDecimal`). Frontend React 18 + TypeScript + Vite, React Query, React Router, React Hook Form + Zod, TanStack Table. Docker + Docker Compose. Auth JWT + BCrypt, roles: administrador / usuario de carga / solo lectura. Arquitectura **multi-tenant-ready** (columna `tenant_id`) sin exponer multiempresa en UI. Exportaciones: Apache POI (Excel) y OpenPDF (PDF); parsing: OpenCSV, PDFBox.

---

## 📋 PROMPT PARA EL MODELO (copiar desde acá hacia abajo)

### Tarea

- Auditar el sistema completo con foco en las garantías críticas. Revisar código y comportamiento de:
- **Integridad contable**: que sea IMPOSIBLE persistir un asiento desbalanceado como confirmado por ninguna vía (UI, API directa, importadores, generators, liquidaciones, migración); consistencia de saldos entre mayores, balance y dashboard; correcta numeración interna; anulaciones que preservan trazabilidad.
- **Multimoneda**: consistencia moneda original/TC/convertido en todo el circuito; diferencias de cambio correctas contra los casos de F3.1/F4.1; ausencia de float/double en importes.
- **Autorización**: cada endpoint con guard correcto; imposibilidad de escalar permisos; reglas de período cerrado inviolables para no-admin.
- **Generadores de asientos**: re-verificar F4.x y F6.x contra TODOS los casos de prueba de F3.1, F4.1, F6.1 y F6.2.
- **OWASP básico**: inyección SQL, IDOR entre tenants (probar acceso cruzado con `tenant_id` ajeno), manejo de JWT, secretos fuera del código, datos sensibles en logs.
- **Auditoría**: que ninguna operación sensible escape al log.
- Producir informe de hallazgos priorizados (bloqueante / alto / medio / bajo) con reproducción y fix sugerido para cada uno.

### Entradas que deben adjuntarse a la sesión

- Sistema completo con datos migrados.
- Casos de prueba de F3.1, F4.1, F6.1, F6.2.

### Salida esperada (definición de terminado)

- Informe de hallazgos priorizados con reproducción y fixes sugeridos.
- Cumple las plantillas y el stack definidos; incluye tests mínimos cuando aplica.
- ⚠️ Este paso tiene **checkpoint humano**: el equipo decide qué hallazgos bloquean el go-live.

### Reglas de negocio que NO se pueden romper en este paso

- Todo asiento debe balancear (Σ debe = Σ haber). Si no balancea, no puede confirmarse; solo puede guardarse como borrador.
- Multimoneda: guardar siempre moneda original + tipo de cambio + importe convertido a ARS; contemplar diferencia de cambio. Nunca float/double para importes.
- El cierre de período evita modificaciones accidentales pero no bloquea consultas, importaciones ni exportaciones. Modificar un período cerrado requiere rol admin y auditoría reforzada.
- Operaciones sensibles registran auditoría: usuario, fecha, hora, acción, datos anteriores y datos nuevos.
- Toda entidad de negocio incluye `tenant_id` y filtro correspondiente, aunque la UI no muestre multiempresa.

### Nota para el operador

Ejecutar con Fable 5: es la última barrera antes del go-live de un sistema contable. Sé adversarial: intentá romper las garantías, no confirmarlas.
