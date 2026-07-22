# F5.1 — Movimientos bancarios y bandeja "pendiente de revisar"

**Paso:** 28 de 55 · **Fase:** F5 — Bancos y conciliación · **Modelo:** Sonnet 5 · **Depende de:** F4.4
**Checkpoint humano:** No.

## Qué se hizo

Primer paso de F5: entidad `MovimientoBancario` ligada a `CuentaBancaria` (F2.4), con una bandeja de revisión donde **nada impacta la contabilidad hasta que el usuario decide explícitamente qué hacer** con cada movimiento pendiente. F5.2 (parsers de Galicia/Mercado Pago/tarjeta) alimentará esta misma bandeja; F5.1 solo cubre la carga manual (`origenImportacion = MANUAL`).

**Flujo de estados**: `PENDIENTE` (único estado "vivo") → una de 4 acciones de resolución, cada una terminal:
- **Confirmar**: usa la cuenta contra-partida ya *sugerida* al cargar el movimiento (campo opcional `cuentaContableSugerida`) y genera un asiento de 2 líneas automáticamente. Si no hay sugerencia, rechaza con `SIN_CUENTA_SUGERIDA` indicando usar "imputar".
- **Imputar**: el usuario elige la cuenta contra-partida en el momento (ignora cualquier sugerencia previa) y genera el mismo tipo de asiento de 2 líneas.
- **Asociar**: vincula el movimiento a un asiento **ya existente y confirmado** (identificado por su número visible, no por id interno) — típicamente el asiento que ya generó un Cobro/Pago de F4.4 para esta misma operación. No genera un asiento nuevo; valida que ese asiento no esté ya asociado a otro movimiento (evita duplicar la conciliación).
- **Descartar**: marca el movimiento como no relevante (ej. duplicado del extracto), con motivo obligatorio. No genera ni vincula ningún asiento — conserva trazabilidad vía auditoría, igual que una anulación.
- **Corregir**: no es una transición de estado — es editar los campos del movimiento mientras sigue `PENDIENTE` (fecha/importe/descripción mal extraídos, cuenta sugerida equivocada, etc.), antes de decidir qué acción tomar.

Los 2 lados del asiento (confirmar/imputar) son: la cuenta contable espejo de la cuenta bancaria (`CuentaBancaria.cuentaContable`, ya obligatoria desde F4.4) vs. la contra-cuenta elegida. El **signo del importe decide el lado**: un ingreso (positivo) debita fondos y acredita la contra-cuenta; un egreso (negativo) es al revés — mismo criterio económico que Cobro/Pago (F4.4), aplicado en ambas direcciones porque acá un único tipo de movimiento cubre depósitos y débitos.

Toda acción queda auditada (`AuditoriaService`, con antes/después) reusando el enum cerrado `AccionAuditoria` existente: `CREAR`/`EDITAR` para alta/corrección, `CONFIRMAR` para las 3 acciones que resuelven el movimiento con vínculo contable (confirmar/imputar/asociar), `ANULAR` para descartar — no hizo falta agregar valores nuevos al enum.

## Decisiones tomadas sin especificación exacta (paso de implementación estándar, sin checkpoint)

El texto del paso lista las 5 acciones por nombre pero no detalla su semántica exacta línea por línea (a diferencia de F4.1, que sí traía fórmulas numéricas). Documentado para trazabilidad:

- **Por qué "confirmar" e "imputar" no son lo mismo**: si fueran idénticas, un requerimiento con 5 acciones distintas tendría solo 4 efectivas. La distinción elegida (cuenta pre-sugerida vs. cuenta elegida en el momento) es la lectura más natural de "confirmar (impacta contabilidad vía asiento)" vs. "imputar (elegir cuenta contable y generar asiento)" — la segunda es explícita sobre "elegir", la primera no.
- **"Asociar" identifica el asiento por número, no por id interno**: el usuario ya ve el número de asiento en toda la UI existente (Cobro/Pago exponen `asientoNumero`); pedirle un id interno sería una fuga de detalle de implementación.
- **Un asiento no puede asociarse a más de un movimiento bancario** (constraint `UNIQUE` en `asiento_id` + chequeo explícito): evita que dos movimientos bancarios distintos queden "conciliados" contra el mismo asiento, lo que rompería la conciliación 1:1 real entre banco y libro.
- **Sin reapertura**: una vez `CONCILIADO`/`DESCARTADO`, no hay acción para volver a `PENDIENTE` — mismo criterio conservador que `EstadoDocumento` (borrador/confirmado/anulado) del resto del sistema. Si se necesitara corregir un movimiento ya conciliado, es un caso de ajuste manual (asiento `AJUSTE`), no de este flujo.

## Verificación realizada

- **Compilación** y **suite completa** dentro de contenedor `maven:3.9-eclipse-temurin-21`: limpia. 279/280 tests (el único que falla es el Testcontainers/Docker Desktop local ya conocido).
- **12 tests nuevos**: creación con cálculo de `importeArs`, rechazo de importe cero, `confirmar` sin sugerencia rechaza, `confirmar` de un ingreso y de un egreso generan el asiento con el signo correcto, `imputar` ignora la sugerencia y usa la cuenta elegida, `asociar` vincula sin generar asiento nuevo, `asociar` rechaza un asiento ya asociado o no confirmado, `descartar` con motivo, guarda de estado (ninguna acción aplica sobre un movimiento ya resuelto), `corregir` actualiza campos mientras está pendiente.
- **Verificación end-to-end contra el servidor real** (migraciones V1→V23 desde volumen limpio):
  - Confirmar un egreso (comisión de Mercado Pago, ARS -1.137,54) con cuenta sugerida: asiento balanceado, Haber fondos / Debe cuenta sugerida, exacto.
  - Imputar un ingreso (transferencia sin identificar, ARS 50.000) eligiendo la cuenta en el momento: asiento correcto, Debe fondos / Haber cuenta elegida.
  - Asociar un movimiento a un asiento ya confirmado existente (por número, no por id): vincula sin generar un asiento nuevo. Reintentar asociar el mismo asiento a otro movimiento → rechazado con `ASIENTO_YA_ASOCIADO`.
  - Descartar un movimiento con motivo: queda `DESCARTADO`, conserva el motivo.
  - Contador de pendientes: refleja correctamente cuántos movimientos siguen sin revisar.
  - Guarda de estado: reintentar `confirmar` sobre un movimiento ya `CONCILIADO` → rechazado con `MOVIMIENTO_NO_PENDIENTE`.
  - Auditoría: cada acción (crear/confirmar/asociar/descartar) quedó registrada con estado antes/después completo.
  - Permisos: usuario `LECTURA` real recibe 200 en la lectura de la bandeja, 403 al crear o descartar.
- Frontend: `tsc -b` y `oxlint` limpios sobre `movimientos-bancarios-page.tsx` y el wiring de router/nav (solo los 2 warnings preexistentes en componentes UI compartidos). La verificación visual en navegador quedó cubierta por la verificación exhaustiva a nivel API de arriba, por el mismo gap de CORS contra el puerto de verificación temporal ya documentado en pasos anteriores.
