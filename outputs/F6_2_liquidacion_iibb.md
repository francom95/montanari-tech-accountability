# F6.2 — Liquidación mensual de IIBB (multi-jurisdicción)

**Paso:** 33 de 55 · **Fase:** F6 — Impuestos · **Modelo:** Opus 4.8 · **Depende de:** F6.1
**Checkpoint humano:** SÍ — el contador valida con una liquidación real por al menos 2 jurisdicciones. **Estado: PENDIENTE.**

---

## Parte 1 — Especificación

### 1.0 Definiciones del negocio (respondidas por el usuario)

- **Régimen: Convenio Multilateral.** La base imponible total del período se reparte entre las jurisdicciones por un **coeficiente unificado** por jurisdicción, no se atribuye entera a la jurisdicción de destino de cada factura.
- **Una alícuota por jurisdicción** (la que ya guarda el maestro de Jurisdicciones, F2.1). Sin desglose por actividad.

### 1.1 Estructura: una liquidación con N sub-liquidaciones por jurisdicción

A diferencia de IVA (una sola determinación por período), IIBB determina el impuesto **por jurisdicción**. El modelo:

```
LiquidacionIibb (período: año + mes, estado PL-5, un asiento)
 └─ LiquidacionIibbJurisdiccion (una por jurisdicción activa del maestro)
     ├─ baseTotal      (ventas netas del período — igual para todas, informativo)
     ├─ coeficiente     (Convenio Multilateral; editable; default = participación por destino)
     ├─ baseImponible   (= baseTotal × coeficiente)
     ├─ alicuota        (snapshot del maestro; editable)
     ├─ impuestoDeterminado (= baseImponible × alicuota)
     └─ componentes[]   (deducciones + arrastre + otros)
```

Se crea **una sub-liquidación por cada jurisdicción activa del maestro** — el maestro *es* la lista de jurisdicciones donde la empresa está inscripta (§ "extensible vía maestro F2.1"). Las que no tengan actividad muestran ceros.

### 1.2 Base imponible y el coeficiente de Convenio Multilateral

**baseTotal** = suma de las ventas netas gravadas confirmadas del período (netoGravado de las facturas de venta; las notas de crédito restan, las notas de débito suman). Se calcula desde las facturas de venta, no desde asientos, porque **la jurisdicción vive en la factura** (`jurisdiccionDestino`) y no en la línea de asiento — que no tiene dimensión de jurisdicción.

**coeficiente** por jurisdicción: es el dato propio de Convenio Multilateral (surge de la determinación anual CM05, combinando ingresos y gastos atribuidos a cada jurisdicción). El sistema **no lo calcula** —eso es un proceso anual aparte—: lo guarda como campo **editable por jurisdicción en la liquidación**, con un **default = participación de esa jurisdicción por destino** (ventas cuyo destino es esa jurisdicción / ventas totales). Ese default reproduce la atribución directa por destino que menciona el plan; el usuario lo reemplaza por el coeficiente real de CM cuando corresponde. Es la aplicación de la nota del operador: *"ante ambigüedad normativa, dejá el punto configurable y documentalo"*.

**baseImponible** de la jurisdicción = `baseTotal × coeficiente`. **impuestoDeterminado** = `baseImponible × alicuota`.

Las facturas **sin jurisdicción de destino** (el campo es nullable) no se pueden atribuir: se informa cuántas hay y qué neto quedó fuera, como advertencia.

### 1.3 Deducciones: manuales por jurisdicción (decisión documentada)

El plan pide contemplar: **retenciones y percepciones sufridas, SIRCREB, pagos a cuenta, saldos a favor anteriores**, todo por jurisdicción.

- **Saldo a favor anterior**: automático. Arrastre del saldo a favor de esa **misma jurisdicción** en la liquidación confirmada del mes anterior (igual criterio que F6.1: dato explícito y auditable, no re-derivado).
- **Percepciones / retenciones / SIRCREB / pagos a cuenta**: **manuales por jurisdicción**, empiezan en cero. Motivo: su atribución automática a una jurisdicción es genuinamente ambigua —la línea de asiento que las registra (cuenta 1.1.2008) **no lleva jurisdicción**, y el SIRCREB es un régimen de recaudación bancaria cuya asignación a una jurisdicción es en sí misma un criterio—. Para no atribuir mal, se cargan a mano. Como ayuda, la pantalla muestra el **total de movimientos de IIBB del período en la cuenta 1.1.2008** (percepciones + SIRCREB) para que el usuario sepa cuánto repartir. *Punto del checkpoint.*

### 1.4 Resultado por jurisdicción

IIBB tiene **una sola etapa** (a diferencia del art. 24 de IVA, con saldo técnico vs. libre disponibilidad). Por jurisdicción:

```
neto = impuestoDeterminado + Σ aportes de componentes   (las deducciones aportan en negativo)
neto > 0  →  saldo a pagar = neto        saldo a favor = 0
neto ≤ 0  →  saldo a pagar = 0           saldo a favor = −neto
```

El **saldo a favor** de una jurisdicción es su propio arrastre del mes siguiente (por jurisdicción, independiente).

### 1.5 Asiento al confirmar (PL-4, `OrigenAsiento.LIQUIDACION_IIBB`, ya predeclarado)

Un asiento para todo el período, con líneas **por jurisdicción** (misma cuenta, jurisdicción en la leyenda, para trazabilidad). Por jurisdicción:

```
DEBE    5.3.2009  Impuesto a los Ingresos Brutos  impuesto determinado
  HABER 1.1.2008  IIBB SIRCREB a favor            deducciones aplicadas (percep. + SIRCREB + ret. + pagos + arrastre)
  HABER 2.1.2010  IIBB a pagar                    saldo a pagar          [si > 0]
DEBE    1.1.2008  IIBB SIRCREB a favor            saldo a favor a arrastrar   [si el resultado es a favor]
```

Misma **regla única** que F6.1: aporte positivo → debe, negativo → haber, resultado del lado opuesto. El impuesto determinado es siempre un gasto (Debe 5.3.2009); las deducciones son créditos ya existentes en 1.1.2008 que se consumen (Haber); solo el neto va al pasivo (2.1.2010). Balancea algebraicamente por construcción, en los dos casos (a pagar / a favor).

Ninguna cuenta nueva: `5.3.2009`, `2.1.2010` y `1.1.2008` ya existen desde el seed de F3.3. Solo se agregan los mapeos concepto→cuenta que faltaban.

### 1.6 Qué se reutiliza de F6.1 y qué es nuevo

**Reutilizado** (arquitectura, no código literal): molde PL-5 de ciclo de vida (borrador → confirmado → anulado), componentes con `tipo`/`signo`/`concepto` y ajuste manual auditado (`importeCalculado` + `importeAjuste` + `motivoAjuste`), regla única del generador de asiento, previsualización sin persistir, des-confirmar = anular (solo admin), sin unique constraint de período (para poder rehacer tras anular).

**Nuevo respecto de IVA**: la dimensión **jurisdicción** (una sub-liquidación por jurisdicción); la base imponible se calcula desde **facturas** (no desde asientos), porque la jurisdicción vive en la factura; el **coeficiente de Convenio Multilateral**; una sola etapa de resultado (no aplica el desdoblamiento del art. 24 de IVA, así que no se reusa `ResultadoIva` — el cálculo por jurisdicción es más simple).

### 1.7 Puntos para el checkpoint del contador

1. **Coeficiente de Convenio Multilateral**: hoy es un campo editable por jurisdicción con default por destino. ¿Alcanza, o hace falta cargar los coeficientes CM05 y mantenerlos? ¿Se cargan una vez por año?
2. **Deducciones manuales por jurisdicción**: ¿está bien cargarlas a mano, o el contador espera una atribución automática (y con qué criterio, dado que el asiento no lleva jurisdicción)?
3. **Pagos a cuenta / anticipos de IIBB**: hoy se tratan como una deducción más contra 1.1.2008. ¿Corresponde una cuenta propia de anticipos?
4. **Base imponible**: ¿el neto gravado es la base correcta, o IIBB grava además conceptos no gravados de IVA en alguna jurisdicción?

---

## Parte 2 — Implementación

### 2.1 Qué se construyó

**Backend** — paquete nuevo `impuestos.iibb`:

| Pieza | Rol |
|---|---|
| `TipoComponenteIibb` | Enum de deducciones (percepciones, retenciones, SIRCREB, pagos a cuenta), arrastre y OTRO, con su signo y concepto. Una sola etapa. |
| `CalculoIibbService` | Motor: base total desde facturas, reparto por coeficiente (default por destino), impuesto determinado por jurisdicción, arrastre por jurisdicción. |
| `CalculoIibb` | Record de salida del motor (base total, deducciones disponibles, jurisdicciones sugeridas, advertencias). |
| `LiquidacionIibb` / `LiquidacionIibbJurisdiccion` / `LiquidacionIibbComponente` | Entidades (PL-5): cabecera → sub-liquidación por jurisdicción → deducciones. |
| `LiquidacionIibbAsientoGenerator` | Asiento PL-4 con líneas por jurisdicción; regla única de F6.1. |
| `LiquidacionIibbService` | Ciclo de vida: crear, recalcular, editar jurisdicción (coeficiente/alícuota), ajustar/agregar/quitar componentes, confirmar, anular. |
| `LiquidacionIibbController` | REST. Confirmar es ADMIN/CARGA; **anular es solo ADMIN**. |

**Migración** `V29__impuestos_liquidacion_iibb.sql`: **ninguna cuenta nueva** (5.3.2009, 2.1.2010, 1.1.2008 ya existían sin uso desde el seed de F3.3); solo los mapeos `IMPUESTO_IIBB_DETERMINADO`→5.3.2009, `IIBB_A_PAGAR`→2.1.2010, `IIBB_SALDO_A_FAVOR`→1.1.2008, y las tres tablas.

**Frontend**: `liquidacion-iibb-page.tsx` en `/impuestos/iibb` — previsualización con el reparto por jurisdicción, tarjeta por jurisdicción con coeficiente y alícuota editables, deducciones cargables por jurisdicción, total del período, confirmar y des-confirmar.

### 2.2 Diferencia de motivo de ajuste respecto de F6.1

En IVA todo ajuste manual exige motivo, porque corrige un valor que el sistema calculó. En IIBB las deducciones son **ingreso de datos** (el sistema no las conoce): cargarlas no pide motivo. El motivo se exige solo al **corregir el arrastre** (lo único que el sistema sí calculó por jurisdicción). Regla: motivo obligatorio si `importeCalculado != 0 && importeAjuste != 0`.

### 2.3 Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **411 tests, 410 en verde** (el único fallo es el de Testcontainers/Docker Desktop local, ajeno a este paso). De esos, **17 nuevos**: `CalculoIibbServiceTest` (6), `LiquidacionIibbAsientoGeneratorTest` (4), `LiquidacionIibbServiceTest` (7).
- El caso que pide el plan —un mes con ≥2 jurisdicciones— está cubierto como test unitario y verificado E2E.
- Frontend: `tsc -b` y `oxlint` limpios.
- **E2E vía docker-compose** (migraciones V1→V29 desde volumen limpio, MySQL 8 real), 2 jurisdicciones:

| # | Escenario | Resultado |
|---:|---|---|
| 3 | Previsualizar marzo con 600.000 a CABA (3%) y 400.000 a BA (4%) | base total **1.000.000**; CABA coef 0.6 → base 600.000 → **18.000**; BA coef 0.4 → base 400.000 → **16.000** |
| 4 | Crear borrador | mismos valores, total a pagar **34.000** |
| 5 | Cargar 5.000 de percepciones en CABA | CABA a pagar **13.000**, total **29.000** |
| 6 | Editar coeficiente de CABA a 0.5 (CM real) | base **500.000**, determinado **15.000**, a pagar **10.000** |
| 7 | Confirmar | asiento con **líneas por jurisdicción**, Σdebe = Σhaber = **31.000** |
| 8 | Abril: BA con 100.000 (det 4.000) y 10.000 de percepciones | BA **saldo a favor 6.000** |
| 9 | Previsualizar mayo | BA arrastra su **saldo a favor de 6.000 por jurisdicción** |

El escenario 6 prueba el coeficiente de Convenio Multilateral editable, el 7 el asiento por jurisdicción balanceando, y el 9 el arrastre independiente por jurisdicción.

- `docker compose down -v` y puertos revertidos (3308→3307, 8083→8081) sin diff en `docker-compose.yml`.

---

## ⚠️ Checkpoint humano — PENDIENTE

El paso requiere que **el contador valide una liquidación real por al menos 2 jurisdicciones**. Cuatro puntos concretos (detalle en §1.7):

1. **Coeficiente de Convenio Multilateral**: hoy editable por jurisdicción con default por destino. ¿Alcanza, o hace falta un maestro de coeficientes CM05 mantenido anualmente?
2. **Deducciones manuales por jurisdicción**: ¿está bien cargarlas a mano, o se espera atribución automática (y con qué criterio)?
3. **Pagos a cuenta / anticipos de IIBB**: hoy son una deducción contra 1.1.2008. ¿Cuenta propia de anticipos?
4. **Base imponible = neto gravado**: ¿es correcto, o IIBB grava conceptos no gravados de IVA en alguna jurisdicción?
