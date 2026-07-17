# F3.3 — Seed del plan de cuentas inicial

**Paso:** 18 de 55 · **Fase:** F3 — Núcleo contable · **Modelo:** Haiku 4.5 (ejecutado con Opus 4.8 a pedido) · **Depende de:** F3.2
**Checkpoint humano:** ✅ Sí — el contador debe revisar el plan cargado antes de avanzar.

## Qué se hizo

Se transcribió la hoja **"Plan de Cuentas"** del Excel `Contabilidad 2026` a una migración Flyway de datos:

- **Archivo:** `backend/src/main/resources/db/migration/V17__contabilidad_seed_plan_de_cuentas.sql`
- **Test:** `backend/src/test/java/com/montanaritech/contable/contabilidad/cuentacontable/SeedPlanDeCuentasIT.java`

### Contenido del seed (tenant_id = 1)

| Entidad | Cantidad |
|---|---|
| Categorías contables | 6 (Activo, Pasivo, Patrimonio Neto, Resultado Positivo, Resultado Negativo, **Otros Resultados**) |
| Rubros | 14 |
| Cuentas contables | 72 (15 madre + 57 imputables) |

> **Cambios de diseño en F3.3 (a pedido del negocio):**
> 1. **Códigos de categoría más cortos:** `RPLUS` → **`RP`**, `RMINUS` → **`RN`** (nombres visibles siguen siendo "Resultado Positivo"/"Negativo").
> 2. **6ta categoría `OTROS_RESULTADOS` "Otros Resultados".** La madre `5.4` se movió a la raíz **`6 "Otros Ingresos y Egresos"`**, fuera de Resultado Negativo. **Solo la madre** lleva la categoría `OTROS_RESULTADOS`; sus **hijas se clasifican `RP`/`RN` según su tipo** (Ganados → RP/ACREEDOR; Comisiones bancarias y MP → RN/DEUDOR). Para permitirlo se **relajó la validación de F3.2** (`CuentaContableService.validarNaturalezaCoincide`): una madre/rubro `OTROS_RESULTADOS` admite hijas `RP` o `RN`. Se corrigió "Otro" → "Otros".
> 3. **Madre intermedia `3.1 "Patrimonio Neto"`** (no está en el Excel): `Capital Social` y `Resultados Acumulados` ahora cuelgan de `3.1`, y `3.1` de `3` — dando el nivel intermedio que ya tienen Activo (`1.1`) y Pasivo (`2.1`).

## Decodificación de códigos (importante)

En el Excel, la columna **Código** tenía muchos valores **mal interpretados por Excel como fechas** (formato de celda `d.m.yyyy` / `d.m`). Ejemplo: la cuenta *Banco Galicia CC* aparecía como `1/1/2001`. El código real se recupera de forma **determinística** desde el valor+formato de celda:

- Celda `datetime(Y,M,D)` con formato `d.m.yyyy` → código `D.M.Y` (ej: `datetime(2004,1,1)` → **`1.1.2004`**).
- Celda con formato `d.m` → código `D.M` (ej: `datetime(2026,1,2)` → **`2.1`** Pasivo Corriente).
- Nivel 1 (número entero `1..5`) → código `1`..`5`.

La validación cruzada confirma el esquema: los sub-códigos que Excel **sí** dejó como texto (`1.1.2004.01`, `.02`, `.03`) cuelgan exactamente de la madre `1.1.2004` decodificada. No hubo ninguna ambigüedad de código.

## Reglas de negocio aplicadas

- **Madre vs imputable** se determina por la existencia de hijos: si algún código empieza con `{code}.`, la cuenta es **madre** (`imputable = FALSE`); si no, es **imputable**. Coincide con la regla innegociable: *las cuentas madre solo agrupan; solo las imputables reciben movimientos*.
- **Rubro:** solo las imputables llevan rubro (obligatorio); las madre quedan con `rubro_id = NULL`.
- **Naturaleza** de una imputable = su columna "Tipo de Cuenta" del Excel; de una madre = la rama de su código (`1→ACTIVO … 5→RMINUS`).
- **Saldo esperado** derivado de la naturaleza: `ACTIVO`, `RMINUS` → **DEUDOR**; `PASIVO`, `PN`, `RPLUS` → **ACREEDOR**.

## Verificación realizada

Se aplicó **toda la cadena V1→V17 sobre un MySQL 8.0 real** (mismo motor que producción/Testcontainers) y se corrieron consultas de invariantes:

| Chequeo | Resultado |
|---|---|
| Todas las migraciones aplican sin error | ✅ |
| Imputables sin rubro | 0 ✅ |
| Madres con rubro | 0 ✅ |
| Cuentas huérfanas (no raíz sin padre) | 0 ✅ |
| Cuentas colgadas de una imputable | 0 ✅ |
| Saldo esperado incoherente con naturaleza | 0 ✅ |
| Acentos/UTF-8 (Crédito, Débito, Diseñador…) | correctos ✅ |

## Decisiones del checkpoint — RESUELTAS

1. **~~Patrimonio Neto sin nivel intermedio.~~ RESUELTO:** se creó la madre intermedia `3.1 "Patrimonio Neto"`.
2. **~~Cuentas de resultado positivo bajo una madre de resultado negativo.~~ RESUELTO:** se creó la categoría "Otros Resultados" y la rama `6` (ver recuadro arriba).

### Otras observaciones menores

- Los **rubros reales del Excel** para las cuentas de resultado son más gruesos que la lista sugerida en el paso F3.3 (el Excel usa `Gastos Operativos`, `Ingresos por servicios brutos`, `Otros Ingresos y Egresos`, `Costos de prestacion por servicios`). Se respetó el Excel (regla: no inventar). Si el contador quiere la apertura más fina (Gastos de Comercialización / Administración / Financieros / Impuestos / Comisiones / Suscripciones / Intereses como rubros), se reasignan en un paso posterior.
- ~~La cuenta madre `5.4` figura en el Excel como "Otro Ingresos y Egresos"~~ → **corregido a "Otros Ingresos y Egresos"** (ahora rama `6`).
- Se transcribieron **saldos DEBE/HABER** solo a nivel de estructura del plan; los **saldos iniciales** (asiento de apertura) corresponden al paso **F10.3**, no a este.
