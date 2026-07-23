# F6.1 — Liquidación mensual de IVA

**Paso:** 32 de 55 · **Fase:** F6 — Impuestos · **Modelo:** Opus 4.8 · **Depende de:** F4.4, F5.3
**Checkpoint humano:** SÍ — el contador valida contra una liquidación real de un mes. **Estado: PENDIENTE.**

---

## Parte 1 — Especificación

### 1.1 De dónde salen los números: de los asientos, no de las facturas

La decisión estructural de este paso. El motor **no** consulta `factura_venta` / `factura_compra` / `comprobante_tributo`: consulta `AsientoLinea` filtrando por cuenta contable, sobre asientos **confirmados** del período.

Cinco razones, en orden de peso:

1. **La regla de crédito fiscal condicional ya se aplicó al generar el asiento.** F4.3 decide si una compra computa crédito fiscal (`tipoComprobante != FACTURA_C && proveedor.condicionIva == RESPONSABLE_INSCRIPTO`); si no computa, el IVA se absorbe en el costo y **nunca toca la cuenta 1.1.2006**. Leer de la cuenta hereda esa regla sin reimplementarla — y sin riesgo de que las dos implementaciones se desincronicen.
2. **Unifica las tres fuentes de crédito fiscal que pide el plan** (facturas de compra + comisiones bancarias identificadas en conciliación F5.3 + otros asientos marcados como con crédito fiscal) en **una sola consulta**. No hay "marca" que mantener: imputar a la cuenta de crédito fiscal *es* la marca.
3. **Las notas de crédito ya invierten lados** en `FacturaVentaAsientoGenerator` / `FacturaCompraAsientoGenerator`. Sumando `debe`/`haber` con signo, el neteo sale solo — sin un `if` por tipo de comprobante.
4. **Multimoneda resuelto de arriba.** `AsientoLinea.debe`/`haber` ya están en ARS (la conversión ocurrió al generar el asiento, con su tipo de cambio y su diferencia de cambio). El motor de IVA no vuelve a convertir nada.
5. Es el mismo patrón que ya probó F5.3 con `AsientoLinea.cuentaBancaria`: **no preguntar de qué documento vino, preguntar qué cuenta movió.**

El documento funcional §8.2 pide explícitamente que el cálculo se nutra de *"facturas de venta; facturas de compra; asientos contables; comisiones bancarias; percepciones de IVA"* — las cinco fuentes llegan por esta única vía.

**Contrapartida asumida:** un asiento manual mal imputado a 1.1.2006 entra al cálculo. Es el comportamiento correcto (el documento fuente pide que los asientos contables alimenten el IVA), y la pantalla muestra el detalle línea por línea con su documento de origen para que sea detectable.

### 1.2 Componentes

| Componente | Signo | Origen | Cuenta |
|---|:---:|---|---|
| Débito fiscal | **+** | `haber − debe` en la cuenta de `IVA_DEBITO_FISCAL` | 2.1.2008 |
| Crédito fiscal | **−** | `debe − haber` en la cuenta de `IVA_CREDITO_FISCAL` | 1.1.2006 |
| Percepciones y retenciones de IVA sufridas | **−** | `debe − haber` en la cuenta de `PERCEPCION_IVA_SUFRIDA` | 1.1.2007 |
| Saldo técnico del período anterior | **−** | `saldoAFavor` de la liquidación confirmada del mes anterior | 1.1.2014 |
| Restituciones | **+** | Solo manual (ver §1.5) | la que indique el usuario |
| Otros conceptos | ± | Solo manual | la que indique el usuario |

Todos los componentes automáticos excluyen los asientos de `origen = LIQUIDACION_IVA`: sin esa exclusión, el propio asiento de liquidación —que mueve esas mismas tres cuentas— se contaría a sí mismo en el período siguiente.

`PERCEPCION_IVA_SUFRIDA` y `RETENCION_IVA_SUFRIDA` mapean ambos a **1.1.2007** (seed V21), así que una sola consulta captura percepciones de compra, retenciones sufridas en cobros y percepciones bancarias imputadas en la conciliación de F5.3.

### 1.3 Resultado

```
resultado = débito fiscal
          − crédito fiscal
          − percepciones
          − saldo técnico anterior
          + restituciones
          ± otros conceptos manuales

resultado  > 0  →  saldo a pagar = resultado        saldo a favor = 0
resultado <= 0  →  saldo a pagar = 0                saldo a favor = −resultado
```

El saldo a favor de un período es el saldo técnico anterior del siguiente. Un único acumulador — ver la limitación documentada en §1.6.

### 1.4 Asiento al confirmar (PL-4, `OrigenAsiento.LIQUIDACION_IVA`)

```
DEBE    2.1.2008  IVA Débito Fiscal            débito fiscal
DEBE    1.1.2014  IVA Saldo a Favor            saldo a favor nuevo      [si > 0]
  HABER 1.1.2006  IVA Crédito Fiscal           crédito fiscal
  HABER 1.1.2007  Percepción de IVA a computar percepciones
  HABER 1.1.2014  IVA Saldo a Favor            saldo técnico anterior   [si > 0, consume el arrastre]
  HABER 2.1.2009  IVA Saldo a Pagar            saldo a pagar            [si > 0]
```

El asiento **cancela** los movimientos de IVA del período contra el resultado. Balancea algebraicamente en los dos casos, sin necesidad de una línea de ajuste:

- **A pagar** (`r > 0`): Debe `= D`; Haber `= C + P + A + (D − C − P − A) = D` ✓
- **A favor** (`r ≤ 0`): Debe `= D + (C + P + A − D) = C + P + A`; Haber `= C + P + A` ✓

Los componentes manuales (restituciones, otros) aportan su propia línea con la cuenta que indicó el usuario, y el resultado absorbe la diferencia — por eso el balance se mantiene y por eso **la cuenta contable es obligatoria en todo componente manual**: sin ella el asiento no podría balancear y la liquidación no se puede confirmar.

Un ajuste manual sobre un componente automático no rompe el balance: cambia el importe de esa línea y el resultado se recalcula absorbiendo la diferencia.

### 1.5 Ajustes manuales

Cada componente guarda `importeCalculado` (del sistema, inmutable) + `importeAjuste` (delta) + `motivoAjuste`, y expone `importeFinal = calculado + ajuste`. Guardar el calculado por separado es lo que hace auditable el ajuste: se ve qué dijo el sistema y qué decidió la persona.

- **Motivo obligatorio** si `importeAjuste != 0`.
- Se pueden **agregar** componentes nuevos (tipo `RESTITUCIONES` u `OTRO`), con cuenta contable obligatoria.
- Solo se ajusta en estado `BORRADOR`. Confirmada, la liquidación es inmutable.
- Toda modificación pasa por `AuditoriaService` (antes/después), igual que el resto del sistema.

**Restituciones** quedan como componente exclusivamente manual: no hay en el sistema ninguna fuente de datos que las identifique automáticamente, y su tratamiento normativo (reintegro de crédito fiscal oportunamente computado vs. devolución de percepciones) depende del caso concreto. Siguiendo la nota del plan —*"ante ambigüedad normativa, dejá el punto como parámetro configurable y documentalo"*— no se resuelve por cuenta propia. **Para el checkpoint del contador.**

### 1.6 Limitaciones y decisiones a validar con el contador

Las tres cosas que hay que mirar en el checkpoint humano:

1. **Un solo acumulador de saldo a favor.** El usuario indicó que el saldo a favor "se arrastra al mes siguiente", sin distinguir saldo técnico de saldo de libre disponibilidad. La normativa argentina sí los distingue (art. 24 Ley 23.349): el **saldo técnico** (crédito fiscal > débito fiscal) solo se computa contra IVA futuro, mientras que el saldo de **libre disponibilidad** (originado en retenciones y percepciones que exceden el impuesto determinado) es además compensable con otros impuestos, transferible y en ciertos casos devolvible. Como Montanari Tech **sí sufre percepciones habitualmente**, es probable que en la realidad existan ambos. Este sistema es de gestión interna y no presenta ante ARCA, por lo que un único acumulador es defendible; pero **separarlos después requiere recalcular los períodos históricos**, así que conviene decidirlo antes de cargar meses reales. *Punto 1 del checkpoint.*
2. **Restituciones sin cálculo automático** (§1.5). *Punto 2 del checkpoint.*
3. **Liquidar un mes sin haber liquidado el anterior no se bloquea**: el arrastre entra en 0 y la pantalla emite una advertencia visible. Bloquearlo impediría arrancar el sistema (el primer mes no tiene anterior) y chocaría con la importación histórica de F4.6. El usuario puede cargar el arrastre a mano como ajuste. *Punto 3 del checkpoint.*

### 1.7 Ciclo de vida

`BORRADOR → CONFIRMADO → ANULADO`, one-way, con `TransicionEstadoValidator` (PL-5).

"Des-confirmar" (solo `ADMINISTRADOR`) se implementa como **anular**: anula el asiento vía `asientoService.anularPorDocumento` (regla F3.1, marca + motivo, conserva trazabilidad) y libera el período. Rehacer la liquidación significa crear una nueva del mismo período — no se vuelve a `BORRADOR`, porque PL-5 prohíbe las transiciones hacia atrás. **Limitación conocida:** los ajustes manuales cargados en la liquidación anulada no se copian a la nueva; hay que recargarlos.

Un período tiene a lo sumo una liquidación en `BORRADOR` o `CONFIRMADO` a la vez (validado en el servicio, no por unique constraint, justamente para permitir re-liquidar después de anular).

---

## Parte 2 — Implementación

### 2.1 Qué se construyó

**Backend** — paquete nuevo `impuestos.iva`:

| Pieza | Rol |
|---|---|
| `TipoComponenteIva` | Enum con el **signo** de cada componente y su `ConceptoContable`. El signo es lo que hace que el resultado y el asiento se deriven solos. |
| `CalculoIvaService` | Motor: lee `AsientoLinea` por cuenta, arma componentes con detalle trazable, resuelve el arrastre. |
| `CalculoIva` | Record de salida del motor (componentes + detalle + advertencias), antes de persistirse. |
| `LiquidacionIva` / `LiquidacionIvaComponente` | Entidades (PL-5). El componente guarda `importeCalculado` y `importeAjuste` por separado. |
| `LiquidacionIvaAsientoGenerator` | Asiento PL-4 con la regla única "aporte positivo al debe, negativo al haber". |
| `LiquidacionIvaService` | Ciclo de vida: crear, recalcular, ajustar, agregar/quitar conceptos, confirmar, anular. |
| `LiquidacionIvaController` | REST. Confirmar es ADMIN/CARGA; **anular es solo ADMIN**. |

**Migración** `V27__impuestos_liquidacion_iva.sql`: cuenta nueva `1.1.2014 IVA Saldo a Favor`, mapeos de `IVA_SALDO_A_PAGAR` (a la `2.1.2009` que ya existía sin uso desde el seed) e `IVA_SALDO_A_FAVOR`, y las dos tablas.

**Frontend**: `liquidacion-iva-page.tsx` en `/impuestos/iva` — previsualización del período, tabla de componentes con ajuste inline + motivo, alta de conceptos manuales, confirmación y des-confirmación, y las advertencias en un bloque destacado.

### 2.2 Bug real encontrado en la verificación E2E

`AsientoLinea.moneda` es `nullable = false`, y `AsientoService.registrarAutomatico` la resuelve **sin guard de null** — es la única dimensión sin él (proyecto, etapa, cliente, proveedor y cuenta bancaria sí lo tienen). El generador usaba el constructor mínimo de `LineaAsientoGenerada`, que el propio javadoc marca como *"para cuando solo interesa validar balance"*, y deja la moneda nula. Resultado: **los 32 tests unitarios pasaban en verde y confirmar reventaba con un 500** (`InvalidDataAccessApiUsageException: The given id must not be null`), porque ningún test tocaba la persistencia real.

Corregido pasando ARS explícitamente (la liquidación de IVA es siempre en pesos: los importes ya llegaron convertidos en las líneas que alimentaron el cálculo, así que TC = 1). Se agregó el test `todasLasLineasLlevanMonedaTipoDeCambioEImporteOriginal`, que habría atrapado el bug.

### 2.3 Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **376 tests, 375 en verde** (el único fallo es el de Testcontainers/Docker Desktop local, ya conocido y ajeno a este paso). De esos, **33 nuevos**: `CalculoIvaServiceTest` (8), `LiquidacionIvaAsientoGeneratorTest` (9), `LiquidacionIvaServiceTest` (16).
- Los tres casos numéricos que pide el plan están cubiertos como test unitario **y** verificados end-to-end.
- Frontend: `tsc -b` y `oxlint` limpios.
- **E2E vía docker-compose** (migraciones V1→V27 desde volumen limpio, MySQL 8 real), 12 escenarios:

| # | Escenario | Resultado |
|---:|---|---|
| 1-2 | Venta 1.000.000 + 21% y compra 400.000 + 21% con percepción de 12.500, confirmadas | IVA 210.000 / 84.000 / percep. 12.500 |
| 3 | Previsualizar marzo | **113.500 a pagar**, con detalle trazable a `FacturaVenta#1` y `FacturaCompra#1` |
| 4 | Crear borrador | mismos importes que la previsualización |
| 5 | Crear otra liquidación del mismo período | rechazado (`LIQUIDACION_IVA_YA_EXISTE`) |
| 6 | Ajustar sin motivo | rechazado (`AJUSTE_IVA_SIN_MOTIVO`) |
| 7 | Ajustar +5.000 con motivo | 118.500 a pagar, recalculado |
| 8 | Confirmar | asiento de 4 líneas, **Σdebe = Σhaber = 215.000** |
| 9 | Re-previsualizar marzo ya confirmado | sigue dando 113.500 → **el asiento de liquidación no se cuenta a sí mismo** |
| 10 | Abril con compra 800.000 > venta 100.000 | **147.000 a favor**, asiento balancea con 1.1.2014 al debe |
| 11 | Previsualizar mayo | **arrastre de 147.000** tomado de abril |
| 12 | Des-confirmar marzo | liquidación y asiento `ANULADO` con motivo; período liberado y rehecho |

El escenario 9 es el que valida la exclusión de `origen = LIQUIDACION_IVA`, y el 11 el arrastre real entre períodos — las dos partes más fáciles de implementar mal.

- `docker compose down -v` y puertos revertidos (3308→3307, 8083→8081) sin diff en `docker-compose.yml`.

### 2.4 Nota sobre el seed de mapeos

Para que el E2E corriera hubo que configurar a mano los mapeos de `CREDITO_POR_VENTA`, `DEUDA_COMERCIAL` y `COSTO_GASTO`, que F4.2/F4.3 dejan **deliberadamente sin default** (dependen del plan de cuentas de cada empresa). No es un problema de este paso, pero conviene tenerlo presente al cargar datos reales: sin esos mapeos no se puede confirmar ninguna factura, y sin facturas confirmadas la liquidación de IVA da todo cero.

---

## ⚠️ Checkpoint humano — PENDIENTE

El paso requiere que **el contador valide el resultado contra una liquidación real de un mes**. Al cierre de este paso todavía no se recibió esa liquidación real, así que la calibración numérica contra un mes verdadero **queda pendiente**.

Tres puntos concretos para esa revisión:

1. **¿Hace falta separar saldo técnico de saldo de libre disponibilidad?** (§1.6, punto 1). Hoy hay un único acumulador. Como Montanari Tech sufre percepciones habitualmente, es probable que en la realidad existan ambos. **Decidirlo antes de cargar meses reales**: separarlos después obliga a recalcular los períodos históricos.
2. **¿Qué son exactamente las "restituciones" en esta operatoria** y de dónde deberían salir? Hoy son un componente exclusivamente manual, sin cálculo automático (§1.5).
3. **¿Está bien que liquidar un mes sin haber liquidado el anterior no se bloquee**, y que el arrastre entre en cero con una advertencia visible? (§1.6, punto 3).
