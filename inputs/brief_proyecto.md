# Brief de orquestación — Sistema de Gestión Contable "Montanari Tech"

> **Para:** Claude Fable 5 (orquestador)
> **De:** Equipo Montanari Tech
> **Objetivo:** Organizar el desarrollo completo de un sistema web de gestión contable, financiera, impositiva y operativa, dividiéndolo en pasos secuenciales y delegando cada paso al modelo más económico capaz de ejecutarlo, minimizando el gasto total de tokens sin sacrificar calidad en las partes críticas.

---

## 1. Instrucciones para Fable 5 (leer primero)

Tu tarea **no es programar todo el sistema en esta corrida**. Tu tarea es:

1. **Descomponer** el proyecto en pasos (fases → tareas → subtareas) ejecutables de forma independiente.
2. **Asignar a cada paso el modelo óptimo** según la matriz de la sección 5 (criterio: usar el modelo más barato que resuelva bien la tarea; reservar los modelos caros solo para diseño de arquitectura, decisiones contables sensibles y revisión final).
3. Para cada paso entregar: `ID`, `Descripción`, `Entradas necesarias`, `Salida esperada`, `Modelo asignado`, `Justificación de 1 línea`, `Dependencias`.
4. Producir un **plan maestro en tabla** + un **backlog detallado** que sirva como cola de trabajo.
5. Marcar explícitamente los **puntos de control humano** (validación del contador, revisión de reglas fiscales argentinas).
6. Optimizar para tokens: agrupar tareas repetitivas (CRUDs, reportes) en plantillas reutilizables para no re-describir el patrón en cada entidad.

No re-expliques el requerimiento de negocio en tu salida; asumí que el equipo ya lo conoce. Andá directo al plan.

---

## 2. Contexto del proyecto (resumen operativo)

Sistema web para reemplazar el Excel contable actual de Montanari Tech por una plataforma centralizada, trazable y automatizada. Uso interno en esta etapa, pero con **arquitectura no atada rígidamente a una sola empresa** (posible evolución a SaaS multiempresa).

**Principio central:** cargar el dato una sola vez y reutilizarlo en todos los módulos. Ej.: una factura de venta genera cuenta a cobrar + IVA débito + ingreso, y alimenta reportes de ventas, IVA, IIBB, resultados, cobranzas, flujo de caja y reporte por proyecto.

**No** emite facturas reales ante ARCA/AFIP en esta etapa: registra comprobantes ya emitidos/recibidos.

**Contexto regional:** contabilidad y fiscalidad **argentina** (IVA, IIBB por jurisdicción, SIRCREB, percepciones/retenciones, Ganancias, Bienes Personales, cargas sociales). Monedas ARS y USD con tipo de cambio configurable y diferencias de cambio.

---

## 3. Stack técnico definido

El stack está fijado por el equipo. Fable 5 debe respetarlo y completar las piezas que falten con opciones estándar y probadas del ecosistema.

### 3.1 Backend
- **Lenguaje/Framework:** Java 21 (LTS) + Spring Boot 3.x.
- **Módulos Spring:** Spring Web (REST), Spring Data JPA, Spring Security, Spring Validation, Spring Boot Actuator (health/metrics).
- **ORM:** JPA / Hibernate.
- **Migraciones de BD:** Flyway (versionado de esquema; imprescindible para un sistema contable trazable).
- **Mapeo DTO↔Entidad:** MapStruct.
- **Reducción de boilerplate:** Lombok.
- **Build:** Maven (o Gradle; Fable elige uno y lo mantiene consistente).
- **Documentación de API:** springdoc-openapi (Swagger UI).

### 3.2 Base de datos
- **Motor:** MySQL 8.x.
- **Manejo de decimales monetarios:** `DECIMAL` (nunca `float/double` para importes; usar `BigDecimal` en Java).
- **Cache/colas (opcional, diferible):** Redis solo si se necesita para alertas/jobs; no en MVP salvo justificación.

### 3.3 Frontend
- **Framework:** React 18+ con **TypeScript**.
- **Build/dev:** Vite.
- **Estado servidor:** TanStack Query (React Query) para data fetching/caché.
- **Ruteo:** React Router.
- **Formularios/validación:** React Hook Form + Zod.
- **UI:** biblioteca de componentes a elección (sugerido: shadcn/ui + Tailwind, o MUI). Fable elige una y la mantiene.
- **Tablas/reportes en pantalla:** TanStack Table.
- **Cliente HTTP:** Axios o fetch con wrapper tipado.

### 3.4 Infraestructura / DevOps
- **Contenedores:** Docker + Docker Compose (servicios: `backend`, `frontend`, `mysql`, y opcionalmente `redis`).
- **Perfiles:** `dev`, `prod` (application-{profile}.yml).
- **Variables sensibles:** vía `.env` / variables de entorno, nunca hardcodeadas.
- **CI (sugerido, no bloqueante):** GitHub Actions para build + tests.
- **Reverse proxy (prod, diferible):** Nginx sirviendo el build de React y proxeando `/api`.

### 3.5 Autenticación y seguridad
- **Auth:** JWT (access + refresh) con Spring Security.
- **Roles:** administrador / usuario de carga / usuario de solo lectura (extensible).
- **Password:** BCrypt.
- **Autorización:** por rol a nivel endpoint + reglas de negocio (ej.: solo admin cierra períodos o edita asientos automáticos).

### 3.6 Generación de reportes exportables
- **Excel:** Apache POI (o EasyExcel para volumen).
- **PDF:** OpenPDF / iText (según licencia) o generación server-side con plantillas.
- **Parsing de importaciones (Excel/CSV/PDF de bancos):** Apache POI (Excel), OpenCSV (CSV), Apache PDFBox (PDF). El parsing de PDF bancario avanzado queda para segunda etapa.

### 3.7 Testing
- **Backend:** JUnit 5 + Mockito; Testcontainers (MySQL) para tests de integración reales.
- **Frontend:** Vitest + React Testing Library.

### 3.8 Piezas agregadas por recomendación (no estaban en el pedido pero se necesitan)
- **Flyway** — versionado de esquema, crítico para trazabilidad contable.
- **springdoc-openapi (Swagger)** — contrato de API para que frontend y backend avancen en paralelo.
- **TypeScript en el frontend** — tipado fuerte para un dominio contable con muchas entidades y montos.
- **BigDecimal + DECIMAL** — corrección monetaria obligatoria.
- **Testcontainers** — para no romper reglas contables silenciosamente en integración.
- **Estructura multi-tenant-ready** — columna/estrategia de tenant preparada aunque la UI no exponga multiempresa aún.

---

## 4. Módulos del sistema (alcance)

1. Gestión Operativa y Maestros (clientes, proveedores, proyectos + etapas, comisionistas, cuentas, tarjetas, monedas, TC, categorías, rubros, costos, vencimientos, presupuestos estimados)
2. Contabilidad (plan de cuentas jerárquico, asientos manuales/automáticos, mayores, balance)
3. Facturación, Cobros y Pagos (facturas venta/compra, cobros/pagos parciales, anticipos, estados, importación histórica)
4. Bancos, Tarjetas y Conciliaciones (cuentas ARS/USD, MercadoPago, importación de resúmenes, conciliación)
5. Reportes (mayores, estado de resultados, balance sumas y saldos, dashboard, control pagos/cobros, reporte por proyecto, ESP, flujo de caja)
6. Impuestos (IVA, IIBB por jurisdicción, ajustes manuales, asientos automáticos)
7. Presupuesto, Vencimientos y Proyección de Caja (calendario, presupuesto de pagos, inversiones Fima)
8. Pendientes Administrativos
9. Seguridad, Usuarios y Auditoría (roles, log de cambios, períodos contables)
10. Transversales: monedas/TC, alertas, búsqueda global "Lupita", migración desde Excel

**Prioridad MVP:** ver sección 17 del documento fuente (alcance primera etapa). El ESP completo y el flujo proyectado avanzado pueden diferirse.

---

## 5. Matriz de delegación de modelos (criterio de ahorro de tokens)

Fable 5 debe asignar cada paso a **uno** de estos perfiles. Regla general: **empezá por el más barato y subí solo si la tarea lo exige.**

| Perfil | Modelo sugerido | Usar para | NO usar para |
|---|---|---|---|
| **Razonamiento pesado** | Opus 4.8 | Diseño de arquitectura global, modelo de datos contable, motor de asientos automáticos, lógica fiscal AR (IVA/IIBB/SIRCREB), diseño de seguridad/roles, revisión final de código crítico | CRUDs simples, texto repetitivo, boilerplate |
| **Trabajo estándar** | Sonnet 5 | Implementación de módulos, endpoints REST, servicios, componentes React complejos, migraciones Flyway, tests de integración, refactors | Tareas triviales masivas, formateo |
| **Volumen barato** | Haiku 4.5 | CRUDs repetitivos entidad por entidad, formularios estándar, validaciones simples, seeds, textos de UI, documentación descriptiva, listados/tablas, tests unitarios básicos | Lógica contable/fiscal, arquitectura, seguridad |

**Heurística de asignación para Fable 5:**
- ¿La tarea define *cómo se estructura* el sistema o *cómo se calcula un impuesto/asiento*? → **Opus 4.8**.
- ¿Implementa una feature con lógica de negocio media? → **Sonnet 5**.
- ¿Es un patrón ya definido que se repite N veces (un CRUD más, un formulario más)? → **Haiku 4.5**.
- Reservá Opus para <15% de los pasos. Si más de eso cae en Opus, revisá si se puede plantillar.

---

## 6. Estructura de salida que espero de Fable 5

### 6.1 Plan maestro (tabla resumen)
Una fila por fase, con: fase, objetivo, modelos predominantes, entregable, dependencias.

### 6.2 Backlog detallado
Para **cada paso**, este formato:

```
### [ID] Título del paso
- **Fase:** 
- **Descripción:** 
- **Entradas:** 
- **Salida esperada:** 
- **Modelo asignado:** [Opus 4.8 | Sonnet 5 | Haiku 4.5]
- **Justificación:** (1 línea)
- **Dependencias:** [IDs previos]
- **Checkpoint humano:** [Sí/No — qué validar]
```

### 6.3 Plantillas reutilizables (clave para ahorrar tokens)
Definí **una sola vez** los patrones que se repiten, para que Haiku los aplique por entidad sin re-explicar:
- **Plantilla CRUD backend** (Entity JPA + Repository + Service + Controller + DTO + MapStruct + validación + migración Flyway), con soft-delete/activar-desactivar y restricción de borrado por movimientos asociados.
- **Plantilla CRUD frontend** (listado con TanStack Table + filtros + formulario React Hook Form/Zod + integración React Query).
- **Plantilla de reporte exportable** (endpoint + servicio + export Excel/PDF, filtros por período/proyecto/cuenta).
- **Plantilla de asiento automático** (evento → líneas debe/haber → validación balance → editable post-generación → registro en auditoría).
- **Plantilla de estados** (borrador / confirmado / anulado).

---

## 7. Fases sugeridas (Fable 5 puede reordenar, pero respetar dependencias)

1. **Fundaciones** — decisiones finales de stack (dentro de lo fijado en sección 3), estructura de repos (mono o multi-repo), Docker Compose base, esquema inicial Flyway, config Spring Security + JWT + roles, esquema de auditoría, estructura multi-tenant-ready sin exponer multiempresa en UI, scaffolding React+Vite+TS. → *mayormente Opus 4.8 / Sonnet 5*.
2. **Maestros** — entidades base y sus CRUDs sobre plantilla. → *Haiku 4.5*; entidades con lógica (proyectos + etapas + presupuesto estimado) *Sonnet 5*.
3. **Núcleo contable** — plan de cuentas jerárquico (cuentas madre no imputables), motor de asientos (manuales, fechas intermedias sin orden cronológico, balanceo obligatorio, numeración interna automática, duplicación), mayores. → *Opus 4.8 diseño / Sonnet 5 implementación*.
4. **Facturación, cobros y pagos** — facturas venta/compra con generación automática de asientos, cobros/pagos parciales, anticipos, multimoneda, importación histórica (Excel/CSV/PDF). → *Opus 4.8 reglas de asiento / Sonnet 5 resto*.
5. **Bancos y conciliación** — cuentas ARS/USD + MercadoPago, saldo inicial con fecha, importación de resúmenes (Galicia/MP/tarjetas) como pendientes de revisar, conciliación, tarjetas de crédito. → *Sonnet 5*; parsing de formatos *Sonnet 5/Haiku*.
6. **Impuestos** — IVA e IIBB por jurisdicción, editables antes de confirmar, asientos automáticos, asociación a proyectos. → *Opus 4.8* (lógica fiscal AR).
7. **Reportes y Dashboard** — sobre plantilla de reporte. → *Sonnet 5* los complejos (reporte por proyecto, estado de resultados, balance sumas y saldos), *Haiku 4.5* los listados y exportaciones simples.
8. **Presupuesto / Vencimientos / Proyección / Inversiones / Pendientes** → *Sonnet 5 / Haiku 4.5*.
9. **Transversales** — alertas, búsqueda "Lupita" (búsqueda global multi-entidad), monedas/TC configurable + diferencias de cambio. → *Sonnet 5*.
10. **Migración desde Excel** — mapeo de hojas → entidades, saldos iniciales, importaciones históricas. → *Sonnet 5*; scripts repetitivos *Haiku 4.5*.
11. **Hardening y revisión final** — seguridad, integridad contable (todo asiento balancea), performance de queries, QA, dockerización de prod. → *Opus 4.8* revisión.

---

## 8. Restricciones y reglas de negocio que Fable 5 NO debe perder

- Todo asiento debe balancear (Σ debe = Σ haber); si no balancea, no se confirma (sí borrador).
- Asientos con fecha intermedia permitidos; ordenamiento por fecha del asiento, no por carga.
- Asientos automáticos deben ser editables después de generados, con registro en auditoría.
- Cuentas madre solo agrupan (no imputables); solo cuentas imputables reciben movimientos.
- Estados mínimos: borrador / confirmado / anulado en facturas, pagos, cobros y asientos.
- Saldo inicial con fecha en cuentas/tarjetas para arrancar sin reconstruir toda la historia.
- Importaciones bancarias entran como "pendientes de revisar", nunca como movimiento definitivo automático.
- Multimoneda ARS/USD: guardar moneda original + TC + importe convertido; contemplar diferencia de cambio. Usar `BigDecimal`/`DECIMAL` siempre.
- Períodos contables mensuales: el cierre es control anti-modificación accidental, no debe bloquear consultas/importaciones/exportaciones.
- Reporte por proyecto = pieza clave (rentabilidad real vs presupuestada).
- Auditoría con usuario/fecha/hora/datos anteriores/datos nuevos en operaciones sensibles.

---

## 9. Definición de "terminado" por paso

Un paso se considera completo cuando: (a) cumple su salida esperada, (b) respeta las plantillas y el stack de la sección 3, (c) no rompe las reglas de la sección 8, (d) pasó el checkpoint humano si estaba marcado, y (e) incluye tests mínimos cuando aplica.

---

## 10. Pendiente del equipo

- **Logo:** se enviará luego (no bloquea ningún paso salvo el branding de UI/exportaciones; Fable 5 puede dejar un placeholder).
- **Biblioteca de UI y build tool de backend (Maven/Gradle):** Fable 5 elige y confirma en Fase 1; una vez elegidos, se mantienen consistentes.

---

## 11. Primer entregable que pido a Fable 5

Devolvé el **Plan maestro (6.1) + Backlog detallado (6.2) + Plantillas (6.3)**, con cada paso ya etiquetado con su modelo y justificación. Si detectás que alguna fase concentra demasiados pasos en Opus 4.8, proponé cómo plantillar para bajar costo. No empieces a escribir código todavía: primero el plan aprobable.
