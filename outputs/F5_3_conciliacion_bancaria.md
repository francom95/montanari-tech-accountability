# F5.3 — Conciliación bancaria

**Paso:** 30 de 55 · **Fase:** F5 — Bancos y conciliación · **Modelo:** Sonnet 5 · **Depende de:** F5.2
**Checkpoint humano:** No.

## Qué se hizo

Pantalla de conciliación por cuenta bancaria y período que compara movimientos importados (F5.1/F5.2) contra el sistema (asientos ya confirmados, sea cual sea su origen — Cobro, Pago, u otro movimiento bancario ya resuelto), sugiere matches por **importe exacto + fecha con tolerancia configurable (±N días, default 3)**, y ofrece **imputación rápida** (comisiones bancarias, impuesto Ley 25413, SIRCREB/percepciones) para lo que no matchea. Esta capa **nunca resuelve nada por sí sola**: solo lee y sugiere — las acciones (confirmar/imputar/asociar/descartar) siguen siendo las de F5.1, `ConciliacionService` solo pre-completa qué elegir.

### La clave del matching: `AsientoLinea.cuentaBancaria`

Cobro, Pago (F4.4) y el propio `MovimientoBancarioService` (F5.1) ya setean `cuentaBancariaId` en la línea de fondos de cada asiento que generan. Eso significa que **cualquier asiento del sistema que haya movido esta cuenta, sin importar su origen**, se puede encontrar con una sola consulta (`AsientoLineaRepository.buscarCandidatosConciliacion`), sin necesidad de consultar por separado las tablas de Cobro/Pago. El candidato se excluye si ya está asociado a otro movimiento bancario (`NOT EXISTS ... WHERE m.asiento = a`, la misma regla 1:1 de "asociar" de F5.1).

Match = `(debe - haber)` de la línea de fondos == `movimiento.importeArs`, y `|fecha movimiento - fecha asiento| <= toleranciaDias`. Un asiento nunca se sugiere dos veces: se remueve del pool disponible en cuanto matchea con un movimiento.

### Imputación rápida: reusa F4.1, no inventa un mapeo paralelo

Los conceptos SIRCREB y percepciones bancarias **no suman `ConceptoContable` nuevos**: son la misma percepción de IIBB/IVA "sufrida" que ya resuelven `PERCEPCION_IIBB_SUFRIDA`/`PERCEPCION_IVA_SUFRIDA` en facturas de compra (F4.3) — reusarlas evita mapeo_cuenta duplicado para el mismo crédito fiscal, venga de una factura o de un extracto bancario. Solo hicieron falta 2 conceptos genuinamente nuevos:
- `COMISION_BANCARIA` → cuenta 6.4003 por defecto, con un mapeo específico por `discriminadorTipo="ORIGEN_IMPORTACION"` que resuelve a 6.4004 ("Comisiones Mercado Pago", ya existía desde F1.8) cuando el movimiento viene de Mercado Pago.
- `IMPUESTO_DEBITOS_CREDITOS_BANCARIOS` (Ley 25413) → cuenta nueva 5.3.2012 (no existía ninguna cuenta para esto todavía).

`ClasificadorMovimientoBancario` matchea la descripción del movimiento contra palabras clave (comisión, "25413", SIRCREB/ing. brutos, percep. IVA) y resuelve la cuenta vía `ResolutorCuentas` (F4.1) — si no hay mapeo configurado para el concepto, no rompe: simplemente no sugiere nada (`Optional.empty()`), consistente con que un mapeo faltante nunca es un error de este paso.

### Saldo banco vs. saldo sistema

- **Saldo sistema**: se reusa `MayorService.calcular` (F3.6) directamente sobre la cuenta contable espejo de la cuenta bancaria, hasta `fechaHasta` — cero código nuevo de cálculo de saldo contable.
- **Saldo banco**: saldo inicial de la cuenta + suma de todos los movimientos bancarios no descartados con fecha entre `fechaSaldoInicial` y `fechaHasta` (nuevo, en `ConciliacionService`, ya que `RecalculoSaldoService` sigue siendo el stub que F5.1 dejó sin actualizar — ver checkpoint más abajo).
- `diferencia = saldoBanco - saldoSistema`: una diferencia real y esperable cuando un movimiento bancario no tiene su contraparte confirmada en el sistema todavía (o viceversa) — exactamente lo que la pantalla existe para exponer, no un bug.

### Frontend

`conciliacion-page.tsx`: filtros (cuenta, período, tolerancia), resumen de saldos + diferencia, tabla de movimientos con la sugerencia inline (match → botón "Aceptar"/"Rechazar"; cuenta sugerida → botón "Aplicar"/"Rechazar"). "Rechazar" es puramente de UI (oculta la sugerencia de esa sesión de pantalla, sin llamada al backend) — la sugerencia se recalcula igual la próxima vez que se abra la pantalla, ya que nada se persiste como "descartado" a nivel de sugerencia. Para carga manual, corrección o descarte de un movimiento sin sugerencia, la pantalla linkea directo a la bandeja de F5.1 en vez de duplicar esa UI.

## Verificación realizada

- **Compilación y suite completa** en contenedor `maven:3.9-eclipse-temurin-21`: 322/323 (el único que falla es el Testcontainers/Docker Desktop local ya conocido). 8 tests de `ClasificadorMovimientoBancarioTest`, 7 de `ConciliacionServiceTest` (match dentro/fuera de tolerancia, un asiento no se sugiere dos veces, movimiento sin fecha nunca matchea, movimiento ya conciliado no recibe sugerencias, cálculo de saldo banco excluyendo descartados).
- **Verificación end-to-end vía docker-compose** (migraciones V1→V25 desde volumen limpio):
  - Asiento manual confirmado con línea de fondos en la cuenta bancaria (simulando un Pago) + movimiento bancario con mismo importe y fecha cercana → `matchSugerido` correcto (número de asiento, fecha, descripción).
  - Movimiento bancario "Comision Mantenimiento Cta Cte" sin match → `cuentaSugerida` = 6.4003 "Comisiones bancarias" vía el clasificador.
  - Aceptar el match (`PATCH /movimientos-bancarios/{id}/asociar`) → el movimiento pasa a `CONCILIADO`, la siguiente consulta del resumen ya no ofrece sugerencias para esa fila y muestra el asiento asociado.
  - Saldo banco vs. sistema: expuesta una diferencia real de $5.000 en el escenario de prueba, causada por un movimiento bancario fechado antes de `fechaSaldoInicial` de la cuenta (quedó fuera del cálculo de saldo banco pero sí lo tomó el Mayor) — comportamiento esperado del límite temporal, no un bug.
  - Permisos: usuario `LECTURA` real recibe 200 en `/conciliacion/resumen` (es de solo lectura, mismo criterio que los GET de `MovimientoBancarioController`).
  - Validación: rango de fechas invertido (`fechaDesde` posterior a `fechaHasta`) → 422 `RANGO_FECHAS_INVALIDO`.
- Frontend: `tsc -b` y `oxlint` limpios.

## Gap descubierto, fuera de alcance de este paso

`RecalculoSaldoService.recalcular` (el que alimenta `CuentaBancaria.saldoActual`, usado en la lista de cuentas bancarias y pensado también para F8.3 "flujo de caja") sigue siendo el stub que su propio comentario dice que F5.1 iba a reemplazar: hoy `saldoActual` siempre es igual a `saldoInicial`, ignorando cualquier movimiento bancario desde entonces. F5.3 deliberadamente **no lo tocó** — el cálculo de "saldo banco" de esta pantalla se hizo como una consulta propia y acotada en `ConciliacionService`, para no ensanchar el alcance de este paso a un servicio compartido que también usa F8.3 (todavía no implementado). Vale la pena revisarlo cuando se aborde F8.3.
