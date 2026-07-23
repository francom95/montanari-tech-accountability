# F6.1 — Liquidación mensual de IVA

**Paso:** 32 de 55 · **Fase:** F6 — Impuestos · **Modelo:** Opus 4.8 · **Depende de:** F4.4, F5.3
**Checkpoint humano:** SÍ. **Estado: CERRADO** — los tres puntos abiertos se implementaron (§3.1–3.3) y la calibración numérica contra la hoja real del contador coincide al centavo (§3.6).

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

Cada componente declara en qué **etapa** entra (§1.3), con qué **signo** aporta, y —los automáticos— de qué **lado** de qué cuenta se lee.

**Etapa técnica** (determina el impuesto del período):

| Componente | Signo | Origen | Cuenta |
|---|:---:|---|---|
| Débito fiscal | **+** | **haber** de `IVA_DEBITO_FISCAL` (ventas) | 2.1.2008 |
| Restitución de crédito fiscal | **−** | **debe** de `IVA_DEBITO_FISCAL` (notas de crédito emitidas) | 2.1.2008 |
| Crédito fiscal | **−** | **debe** de `IVA_CREDITO_FISCAL` (compras) | 1.1.2006 |
| Restitución de débito fiscal | **+** | **haber** de `IVA_CREDITO_FISCAL` (notas de crédito recibidas) | 1.1.2006 |
| Saldo técnico del período anterior | **−** | liquidación confirmada del mes anterior | 1.1.2014 |

**Etapa de ingresos directos** (aplica lo retenido y percibido):

| Componente | Signo | Origen | Cuenta |
|---|:---:|---|---|
| Percepciones y retenciones de IVA sufridas | **−** | neto de `PERCEPCION_IVA_SUFRIDA` | 1.1.2007 |
| Saldo de libre disponibilidad del período anterior | **−** | liquidación confirmada del mes anterior | 1.1.2015 |

Más dos tipos manuales (`OTRO_TECNICO` y `OTRO_INGRESO_DIRECTO`) que el usuario agrega con su propia cuenta contable.

**El lado importa y no es un detalle de implementación.** Como los generadores de F4.2/F4.3 invierten los lados de las notas de crédito, el lado *es* lo que distingue una venta de una nota de crédito emitida, sin mirar el tipo de comprobante ni agregar marcas al asiento. Y esa distinción hace falta porque el IVA de una NC emitida **no reduce el débito fiscal sino que aumenta el crédito fiscal** (art. 12 inc. b), y el de una NC recibida aumenta el débito (art. 11, último párrafo) — ver §3.2.

Percepciones y arrastres se leen por el **neto** de su cuenta, no por un lado fijo: una devolución de percepción va del lado opuesto y debe netear.

Todos los componentes automáticos excluyen los asientos de `origen = LIQUIDACION_IVA`: sin esa exclusión, el propio asiento de liquidación —que mueve esas mismas cuentas— se contaría a sí mismo en el período siguiente.

`PERCEPCION_IVA_SUFRIDA` y `RETENCION_IVA_SUFRIDA` mapean ambos a **1.1.2007** (seed V21), así que una sola consulta captura percepciones de compra, retenciones sufridas en cobros y percepciones bancarias imputadas en la conciliación de F5.3.

### 1.3 Resultado: dos etapas, no una suma lineal

El art. 24 de la Ley 23.349 distingue dos especies de saldo a favor que **no se pueden compensar entre sí**, así que el cálculo no puede ser una única suma con signo:

```
Etapa 1 — técnica
  débito fiscal + restitución de débito − crédito fiscal − restitución de crédito
                − saldo técnico anterior
  ├─ > 0 → impuesto determinado, pasa a la etapa 2
  └─ ≤ 0 → SALDO TÉCNICO (art. 24, 1er párrafo)
           solo computable contra débitos fiscales de períodos siguientes

Etapa 2 — ingresos directos
  impuesto determinado − percepciones y retenciones − libre disponibilidad anterior
  ├─ > 0 → SALDO A PAGAR
  └─ ≤ 0 → SALDO DE LIBRE DISPONIBILIDAD (art. 24, 2do párrafo)
           además compensable con otros impuestos, transferible y devolvible
```

**Los dos saldos a favor pueden coexistir** en un mismo mes: más crédito fiscal que débito *y* percepciones sufridas. Lo que sí es excluyente es el saldo a pagar contra los otros dos. Ambos se arrastran al mes siguiente, cada uno a su propio componente.

El cálculo vive en `ResultadoIva`, aparte del servicio, porque lo usan dos caminos —la liquidación persistida y la previsualización— y duplicarlo sería la forma más fácil de que se desincronicen.

### 1.4 Asiento al confirmar (PL-4, `OrigenAsiento.LIQUIDACION_IVA`)

El generador no arma las líneas caso por caso: aplica **una regla única — aporte positivo al debe, negativo al haber** — a todos los componentes, y después imputa cada resultado del lado opuesto (saldo a pagar al haber creando el pasivo; saldos a favor al debe creando el activo que arrastra el mes siguiente).

```
DEBE    2.1.2008  IVA Débito Fiscal                  débito fiscal
DEBE    1.1.2006  IVA Crédito Fiscal                 restitución de débito (NC recibidas)
DEBE    1.1.2014  IVA Saldo a Favor                  saldo técnico nuevo             [si > 0]
DEBE    1.1.2015  IVA Saldo de Libre Disponibilidad  libre disponibilidad nueva      [si > 0]
  HABER 2.1.2008  IVA Débito Fiscal                  restitución de crédito (NC emitidas)
  HABER 1.1.2006  IVA Crédito Fiscal                 crédito fiscal
  HABER 1.1.2007  Percepción de IVA a computar       percepciones
  HABER 1.1.2014  IVA Saldo a Favor                  saldo técnico anterior consumido
  HABER 1.1.2015  IVA Saldo de Libre Disponibilidad  libre disponibilidad anterior consumida
  HABER 2.1.2009  IVA Saldo a Pagar                  saldo a pagar                   [si > 0]
```

Balancea algebraicamente siempre, porque la suma de los aportes **es** `saldo a pagar − saldo técnico − libre disponibilidad` por construcción, y esas tres líneas la contrapesan exactamente. No hace falta una línea de ajuste ni casos especiales.

Los componentes manuales aportan su propia línea con la cuenta que indicó el usuario — por eso **la cuenta contable es obligatoria en todo componente manual**: sin ella el asiento no podría balancear y la liquidación no se puede confirmar. Un ajuste manual sobre un componente automático tampoco rompe el balance: cambia el importe de esa línea y el resultado se recalcula absorbiendo la diferencia.

Un componente en cero no genera línea, para no ensuciar el asiento.

### 1.5 Ajustes manuales

Cada componente guarda `importeCalculado` (del sistema, inmutable) + `importeAjuste` (delta) + `motivoAjuste`, y expone `importeFinal = calculado + ajuste`. Guardar el calculado por separado es lo que hace auditable el ajuste: se ve qué dijo el sistema y qué decidió la persona.

- **Motivo obligatorio** si `importeAjuste != 0`.
- Se pueden **agregar** componentes nuevos (`OTRO_TECNICO` u `OTRO_INGRESO_DIRECTO` segun la etapa a la que afecten), con cuenta contable obligatoria.
- Solo se ajusta en estado `BORRADOR`. Confirmada, la liquidación es inmutable.
- Toda modificación pasa por `AuditoriaService` (antes/después), igual que el resto del sistema.

### 1.6 Decisiones validadas con el contador

Los tres puntos que estaban abiertos en la primera versión quedaron resueltos (fundamento en §3):

1. **Las dos especies de saldo a favor se separan** según el art. 24 (§3.1).
2. **Las notas de crédito se computan por lado de imputación**: la emitida aumenta el crédito fiscal, la recibida aumenta el débito (§3.2). Ya no existe un componente "restituciones" cargado a mano: se calcula solo.
3. **Liquidar un mes sin haber liquidado el anterior no se bloquea**: los arrastres entran en cero y la pantalla emite una advertencia visible. Bloquearlo impediría arrancar el sistema —el primer mes no tiene anterior— y chocaría con la importación histórica de F4.6.

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

## Parte 3 — Correcciones del checkpoint del contador

Los tres puntos abiertos fueron respondidos y **las dos correcciones de fondo ya están implementadas**. Lo que sigue reemplaza lo que decían §1.5 y §1.6 en la primera versión.

### 3.1 Se separan las dos especies de saldo a favor (art. 24, Ley 23.349)

**Respuesta:** separarlos según la ley.

La primera versión tenía un único acumulador. Ahora el cálculo corre en **dos etapas**, porque cada una genera un saldo a favor de especie distinta que la ley no permite mezclar:

```
Etapa 1 — técnica
  débito fiscal − crédito fiscal − saldo técnico anterior
  ├─ > 0 → impuesto determinado, pasa a la etapa 2
  └─ ≤ 0 → SALDO TÉCNICO (art. 24, 1er párrafo)
           solo computable contra débitos fiscales de períodos siguientes

Etapa 2 — ingresos directos
  impuesto determinado − percepciones y retenciones − libre disponibilidad anterior
  ├─ > 0 → SALDO A PAGAR
  └─ ≤ 0 → SALDO DE LIBRE DISPONIBILIDAD (art. 24, 2do párrafo)
           además compensable con otros impuestos, transferible y devolvible
```

**Los dos saldos a favor pueden coexistir** en un mismo mes (más crédito fiscal que débito *y* percepciones sufridas), y ese es justamente el caso que el acumulador único resolvía mal: sumaba en una sola cifra un excedente técnico —que por ley queda cautivo— con percepciones que sí son compensables. El total daba igual; la composición, no.

Infraestructura: cuenta nueva `1.1.2015 IVA Saldo de Libre Disponibilidad`, concepto `IVA_SALDO_LIBRE_DISPONIBILIDAD`, columna `saldo_libre_disponibilidad` y un segundo arrastre (migración `V28`). El asiento imputa cada especie a su cuenta.

El cálculo de las dos etapas vive en una clase propia (`ResultadoIva`) porque lo usan dos caminos —la liquidación persistida y la previsualización—, y duplicarlo sería la forma más fácil de que se desincronicen.

### 3.2 Las notas de crédito se separan por lado de imputación (art. 11 y 12)

**Respuesta del contador:** *"cuando emitís una nota de crédito, el IVA generado no disminuye tu débito fiscal sino que aumenta el crédito fiscal, es por eso que se pone en concepto de restitución de crédito fiscal"*. Y por simetría, la NC recibida aumenta el débito fiscal.

La primera versión estaba mal: neteaba por naturaleza de cuenta, así que una NC emitida **restaba del débito fiscal**. Es el mismo total, pero el art. 12 inc. b) ubica los descuentos y devoluciones otorgados dentro del **crédito fiscal**, no como menor débito — y con las dos especies de saldo separadas, esa diferencia de composición sí cambia los números.

La corrección no necesitó marcas nuevas en el asiento ni mirar el tipo de comprobante: **el lado de la imputación ya lo dice todo**, porque los generadores de F4.2/F4.3 invierten los lados de las notas de crédito.

| Cuenta | Haber | Debe |
|---|---|---|
| 2.1.2008 Débito fiscal | ventas → **débito fiscal** | NC emitidas → **restitución de crédito fiscal** (resta) |
| 1.1.2006 Crédito fiscal | NC recibidas → **restitución de débito fiscal** (suma) | compras → **crédito fiscal** |

Es la misma idea que resolvió la conciliación en F5.3 —preguntar qué cuenta movió, no de qué documento vino— llevada un paso más: preguntar además de qué lado.

Los componentes `PERCEPCIONES` y los arrastres siguen leyéndose por el neto de su cuenta, no por un lado fijo (una devolución de percepción iría del lado opuesto y debe netear).

### 3.3 No se bloquea liquidar un mes si el anterior no se liquidó

**Respuesta:** confirmado, no bloquear. Queda como estaba: los arrastres entran en cero y la pantalla muestra una advertencia visible.

### 3.4 Bug encontrado al aplicar estos cambios

Al refactorizar, `PERCEPCIONES` dejó de calcularse: el filtro de "componentes que se leen de asientos" exigía tener un lado declarado, y ese componente se lee por el neto (lado `null`). Lo detectó el test que verifica que las percepciones de compras, cobros y bancos caigan en un mismo componente. Corregido distinguiendo explícitamente los arrastres en vez de inferirlos por la ausencia de lado.

### 3.5 Verificación de las correcciones

- Suite completa: **393 tests, 392 en verde** (el único fallo sigue siendo el de Testcontainers/Docker Desktop local). Se sumó `ResultadoIvaTest` (9 casos, entre ellos la coexistencia de ambos saldos y la NC emitida computada como crédito) y se reescribieron los tests que asumían la semántica vieja.
- Frontend: la pantalla separa visualmente las dos etapas y muestra los tres resultados con su explicación; `tsc -b` y `oxlint` limpios.
- **E2E vía docker-compose** (migraciones V1→V28 desde volumen limpio, MySQL 8 real), 4 escenarios que cubren exactamente lo que corrigió el contador:

| # | Escenario | Resultado |
|---:|---|---|
| 1 | Marzo: venta 1.000.000 + **NC emitida** 100.000 + compra 400.000 | débito 210.000 (intacto), restitución de crédito 21.000 con aporte **−21.000**, crédito 84.000 → **105.000 a pagar** |
| 2 | Abril: compra 800.000 + **NC recibida** 50.000 + venta 100.000 + percepción 30.000 | restitución de débito 10.500 con aporte **+10.500**; **técnico 136.500 y libre disponibilidad 30.000 conviviendo** |
| 3 | Confirmar abril | asiento con **1.1.2014 y 1.1.2015 por separado**, Σdebe = Σhaber = 198.000 |
| 4 | Previsualizar mayo | **arrastra las dos especies por separado**: 136.500 técnico + 30.000 libre |

El escenario 1 es el que prueba la corrección de las notas de crédito (el débito fiscal no bajó a 189.000), y el 2 el que prueba que los dos saldos son cosas distintas: con el acumulador único habrían salido como 166.500 indistintos.

- `docker compose down -v` y puertos revertidos (3308→3307, 8083→8081) sin diff en `docker-compose.yml`.


### 3.6 Calibración numérica — HECHA. Checkpoint CERRADO.

El contador aportó su hoja real "IVA a pagar" (Excel *Contabilidad 2026*). Su estructura coincide **exactamente** con el motor de dos etapas: débito − restitución de crédito − crédito = saldo técnico (1° párrafo), y saldo técnico − percepciones = saldo a pagar (2° párrafo), con arrastres por párrafo.

**Calibración E2E de JUNIO 2026** (se reprodujeron en el sistema, con asientos manuales sobre las cuentas exactas, los movimientos que implica la hoja, y se corrió la liquidación):

| Componente | Hoja del contador | Motor del sistema |
|---|---:|---:|
| Débito fiscal | 3.148.736,006 | 3.148.736,01 |
| Restitución de crédito fiscal (NC) | −609.000 | −609.000 (aporte) |
| Crédito fiscal | −43.544,48 | −43.544,48 (aporte) |
| Saldo técnico | 2.496.191,526 | 2.496.191,53 |
| Percepciones IVA | −1.800 | −1.800 (aporte) |
| **Saldo a pagar** | **2.494.391,526** | **2.494.391,53** |

**Coinciden al centavo.** La diferencia de milésimas (…,526 vs …,53) es solo porque la hoja lleva 3 decimales sin redondear y el sistema —como la DDJJ real— redondea a centavos. El mapeo de conceptos también quedó confirmado: la "restitución de crédito fiscal" de la hoja se computa como aporte negativo en la etapa técnica, exactamente como el motor lee una NC del debe de la cuenta de débito fiscal.

**Checkpoint de F6.1: cerrado.**
