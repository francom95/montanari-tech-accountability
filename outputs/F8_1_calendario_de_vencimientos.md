# F8.1 — Calendario de vencimientos

Modelo asignado: Haiku 4.5. Ejecutado con Sonnet 5 (confirmado con el usuario al iniciar el paso).

## Qué se construyó

Una entidad `Vencimiento` que centraliza toda obligación de pago futura (impuestos, tarjeta, sueldos, suscripciones, planes de pago, etc.), con:

- CRUD completo + ciclo de vida (marcar pagado, reprogramar, cancelar), auditado.
- Motor de generación automática **on-demand** (sin scheduling) desde 5 fuentes: liquidaciones de IVA/IIBB confirmadas (F6.1/F6.2), tarjetas de crédito activas (F5.4), conceptos recurrentes activos con periodicidad ≠ ÚNICA (F2.1), y vencimientos manuales recurrentes ya resueltos (encadena la próxima ocurrencia).
- Servicio de "próximos vencimientos" (`proximos(dias)`), reusable directamente por F9.1 (alertas) y F8.3 (flujo de caja proyectado) sin pasar por HTTP.
- Frontend: calendario mensual hand-rolled + vista de lista con filtros, formulario de alta/edición, y las 3 acciones de ciclo de vida, todo en `/presupuesto/vencimientos`.

## Decisiones de diseño (confirmadas con el usuario antes de implementar)

1. **Generación automática vía endpoint on-demand**, no un job programado. El proyecto no tenía ninguna infraestructura de scheduling (cero `@Scheduled`) ni patrón de eventos (`@EventListener`/`ApplicationEventPublisher`) en todo el backend — introducir la primera dependencia de cron no era necesario: el frontend llama al endpoint al abrir el calendario, más un botón "Generar ahora" explícito.
2. **`Concepto.periodicidad` (F2.1) pasa de texto libre a un enum `Periodicidad { UNICA, MENSUAL, ANUAL }`** — nunca antes había sido leído por ninguna lógica; ahora es el primer consumidor real. La migración normaliza valores existentes por keyword (`%mensual%`/`%anual%` → el enum correspondiente, cualquier otra cosa incluido NULL → UNICA).

## Diseño del dominio

`Vencimiento` (paquete `vencimientos`, extiende `EntidadNegocio`): `descripcion`, `tipo` (13 valores: IVA, IIBB, GANANCIAS, BIENES_PERSONALES, CARGAS_SOCIALES, SUELDOS, CONTADOR, TARJETA, SUSCRIPCION, PRESTAMO, PLAN_DE_PAGO, PAGO_AUTOMATICO, OTRO), `fecha`, `importeEstimado` (nullable), `moneda` (FK requerida), `recurrencia` (UNICA/MENSUAL/ANUAL/PERSONALIZADA), `estado` persistido (PENDIENTE/PAGADO/REPROGRAMADO/CANCELADO — **VENCIDO nunca se persiste**, se deriva en lectura comparando `fecha` contra hoy, mismo criterio que `EstadoVencimiento` de F4.5), asociaciones opcionales a cuenta contable/proveedor/tarjeta/proyecto/concepto, `liquidacionTipo`+`liquidacionId` (referencia polimórfica sin FK, mismo patrón que `AtribucionImpuesto` de F6.3), `asientoVinculado` (FK a `Asiento`, opcional, se completa al marcar pagado), y `origenGeneracion`+`origenGeneracionRefId` (mecanismo de idempotencia del motor automático).

## Motor de generación automática

`VencimientoService.generarAutomaticos()` — un único método idempotente, 5 fuentes, todas de solo lectura sobre servicios ya cerrados (no se tocó `LiquidacionIvaService`/`LiquidacionIibbService`/`PagoTarjetaService`/`ConceptoService`):

1. **IVA confirmadas**: fecha = día fijo del mes siguiente (`ConfiguracionDashboard.diaVencimientoIva`, reusa la config de F7.5), importe = `saldoAPagar`. Idempotencia: `existsByOrigenGeneracionAndOrigenGeneracionRefId`.
2. **IIBB confirmadas**: análogo con `diaVencimientoIibb`/`saldoAPagarTotal`.
3. **Tarjetas activas**: próxima ocurrencia del día `diaVencimiento` a partir de hoy (mes actual si no pasó, si no el siguiente), clamped al último día del mes; importe = `saldoActual` (ya mantenido por `RecalculoSaldoService`, sin nueva lógica de ciclo de facturación). Idempotencia por `(TARJETA, tarjetaId, fecha)`.
4. **Conceptos activos con periodicidad ≠ ÚNICA**: MENSUAL asegura un vencimiento del mes en curso, ANUAL del año en curso (mismo día que la fecha de creación del concepto, clamped). Idempotencia por rango de fechas.
5. **Vencimientos manuales recurrentes ya resueltos** (PAGADO/CANCELADO): encadena la próxima ocurrencia (+1 mes/+1 año/+N días según recurrencia); `origenGeneracionRefId` del nuevo apunta al resuelto que lo originó, para que corridas repetidas no lo dupliquen.

`POST /api/v1/vencimientos/generar-automaticos` devuelve `{ generados: [{origen, cantidad}], total }`.

## Verificación

- **Backend**: 505 tests, 0 fallas propias (la única falla es el IT de Testcontainers, ambiental — no arranca Docker Desktop local, documentado desde F1). 20 tests nuevos en `VencimientoServiceTest` (CRUD, transiciones de estado válidas/inválidas, las 5 fuentes de `generarAutomaticos()` incluida idempotencia, `proximos()`) + 3 en `VencimientoMapperTest` (cómputo de VENCIDO). Se corrigió de paso una regresión real preexistente de Fase A: `ConceptoServiceTest` seguía pasando `String` literales (`"mensual"`, `"anual"`) donde ahora se espera el enum `Periodicidad` — el `mvn compile` incremental no la había detectado por classes cacheadas; un `mvn clean test-compile` la expuso.
- **Frontend**: `tsc -b` y `oxlint` limpios (solo 2 warnings preexistentes ajenos a este paso).
- **E2E real contra Docker Compose (MySQL 8 real)**: migración V36 aplicada limpia sobre las 36 previas. Se crearon vía API una tarjeta de crédito activa, un concepto MENSUAL activo, y se insertaron directamente en MySQL una `LiquidacionIva` y una `LiquidacionIibb` CONFIRMADAS (para no rehacer todo el flujo de cálculo de F6.1/F6.2, ya cubierto por sus propios tests). `generar-automaticos` generó las 4 correctamente con fechas/importes exactos; una segunda corrida generó 0 (idempotencia confirmada). Se probó el ciclo de vida completo (marcar pagado, reprogramar, cancelar) y `proximos()` excluyendo/incluyendo correctamente. Se probó además el encadenamiento de un vencimiento manual recurrente mensual (pagar la cuota de julio generó automáticamente la de agosto).
- **UI en navegador**: verificado con datos reales — vista Calendario (grilla mensual con los 4 vencimientos ubicados en sus días correctos), vista Lista (filtros + tabla + acciones condicionadas por estado), y la acción "Marcar pagado" ejecutada en vivo desde la UI (el estado cambió de Pendiente a Pagado y las acciones desaparecieron, igual que en el backend).

## Notas de infraestructura (no de este paso)

- **Gap de CORS/proxy** (documentado desde F2.6): el backend no tiene configuración CORS y no hay proxy en `nginx.conf`/`vite.config.ts` — verificar el frontend contra el backend real en el navegador (`docker compose`, orígenes `:5173`/`:8081` distintos) requiere un proxy temporal. Se aplicó uno (`VITE_API_BASE_URL` relativo + `location /api/` en `nginx.conf`) solo para esta verificación y se revirtió antes del commit. Sigue siendo deuda técnica no resuelta.
- El entorno de preview browser tuvo caché agresivo de assets con hash (sirvió bundles viejos incluso en pestañas nuevas hasta usar un query param de cache-busting) — limitación de la sandbox de preview, no del código; documentado por si se repite en pasos futuros.
