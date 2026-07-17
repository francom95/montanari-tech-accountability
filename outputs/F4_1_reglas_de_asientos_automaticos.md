# F4.1 — Reglas de asientos automáticos e imputación de cobros/pagos

**Paso:** 22 de 55 · **Fase:** F4 — Facturación, cobros y pagos · **Modelo:** Opus 4.8 · **Depende de:** F3.1
**Checkpoint humano:** ✅ Sí — el contador valida cada regla de asiento con los casos numéricos.

> **Naturaleza de este paso:** es una **especificación**, no código. Sonnet implementa en F4.2 (facturas de venta), F4.3 (facturas de compra) y F4.4 (cobros y pagos) exactamente lo que está acá. Todo generator entrega un `AsientoGenerado` (molde PL-4, ya scaffoldeado en `common/asiento/`) a `AsientoService.registrarAutomatico(...)` (contrato de F3.1 §8.1), que valida balance, numera y confirma en la **misma transacción** del documento (ADR-07: el generator nunca inserta en `asiento`/`asiento_linea` directamente).

---

## 0. Principios heredados de F3.1 (innegociables acá)

1. **Balance exacto en ARS**: `Σ debe = Σ haber` con `BigDecimal.compareTo` (sin tolerancia). Si no balancea, el documento **no se confirma** (se puede guardar borrador). El generator corre **solo al confirmar** el documento.
2. **Multimoneda por línea** (F3.1 §3.3): `debe`/`haber` son siempre ARS ya convertidos; cada línea preserva `moneda_id`, `importe_original`, `tipo_cambio`, `fuente_tc`. En ARS: `tipo_cambio = 1,000000`, `importe_original = debe+haber`, `fuente_tc = NULL`.
3. **Redondeo** (F3.1 §6.1): cada materialización a ARS es `round2(original × TC)` HALF_UP a 2 decimales, en el momento. Nunca se arrastran más decimales. TC con 6 decimales.
4. **Dimensiones analíticas por línea** (F3.1 §3.1, D-1): `proyecto_id`, `etapa_id`, `cliente_id`, `proveedor_id`, `cuenta_bancaria_id` (destino de fondos). Opcionales, NULL por defecto.
5. **`generada_auto = true`** en toda línea producida por un generator; las líneas agregadas a mano después quedan `false` (F3.1 §4.2 / funcional §5.2). Solo ADMIN edita/quita líneas `generada_auto = true` (ya implementado en F3.5, `AsientoService.editarConfirmado`).
6. **Vínculo bidireccional** documento↔asiento: la cabecera lleva `origen`, `origen_tipo` (nombre de la entidad documento) y `origen_id`. Anulación solo vía documento (`ANULACION_VIA_DOCUMENTO`, F3.1 §4.4 D-3).
7. **Diferencia de cambio a lo percibido** (F3.1 §6.2, D-4): se reconoce únicamente al imputar un cobro/pago a un comprobante en moneda extranjera. Sin revaluación de tenencia (§6.5).

---

## 1. Tabla de mapeo concepto→cuenta

El motor **no hardcodea códigos de cuenta**. Un servicio `ResolutorCuentas` traduce un **concepto** (función contable) al `cuenta_contable` a usar, resolviendo por el contexto de la operación. Editable por el admin (CRUD, rol ADMINISTRADOR).

### 1.1 Esquema (implementa F4.2, primer consumidor)

```sql
CREATE TABLE mapeo_cuenta (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    concepto             VARCHAR(40)  NOT NULL,   -- enum ConceptoContable (§1.3)
    discriminador_tipo   VARCHAR(30),             -- NULL = fila por defecto del concepto
    discriminador_valor  VARCHAR(60),             -- valor del discriminador (código enum o id como texto)
    cuenta_contable_id   BIGINT       NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en            DATETIME(6)  NOT NULL,
    creado_por           VARCHAR(120),
    actualizado_en       DATETIME(6)  NOT NULL,
    actualizado_por      VARCHAR(120),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_mapeo_cuenta UNIQUE (tenant_id, concepto, discriminador_tipo, discriminador_valor),
    CONSTRAINT fk_mapeo_cuenta_cuenta FOREIGN KEY (cuenta_contable_id) REFERENCES cuenta_contable(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

> Nota MySQL: `UNIQUE` trata cada `NULL` como distinto, así que puede haber una sola fila "por defecto" (`discriminador_tipo IS NULL`) por concepto pero no la protege el UK. La unicidad de la fila por defecto se valida en el `MapeoCuentaService` (`MAPEO_DUPLICADO`), no en la BD.

### 1.2 Resolución (`ResolutorCuentas.resolver(concepto, contexto)`)

1. Buscar fila **específica**: `concepto` + `discriminador_tipo` + `discriminador_valor` que matcheen el contexto.
2. Si no hay, buscar fila **por defecto**: `concepto` con `discriminador_tipo IS NULL`.
3. Si tampoco hay → `MAPEO_CUENTA_FALTANTE` (422). El documento se pudo guardar como borrador; solo falla al **confirmar** (coherente con F3.1: sin datos completos no se confirma, se deja borrador).
4. Si la cuenta resuelta no es imputable o está inactiva → `CUENTA_NO_IMPUTABLE` / `CUENTA_INACTIVA` (reusa validación de F3.4).

### 1.3 Catálogo de conceptos (`enum ConceptoContable`)

| Concepto | Discriminador | Cuenta del seed F3.3 (default propuesto) | Naturaleza |
|---|---|---|---|
| `IVA_DEBITO_FISCAL` | — | `2.1.2008` IVA Débito Fiscal | PASIVO |
| `IVA_CREDITO_FISCAL` | — | `1.1.2006` IVA Crédito Fiscal | ACTIVO |
| `INGRESO_VENTA` | `TIPO_INGRESO` = VENTA / OTRA_VENTA | `4.1.2001` Ingresos por ventas / `4.1.2002` Ingresos por otras ventas | RP |
| `CREDITO_POR_VENTA` (CxC) | por **cliente** (§2) | `1.1.2004.01`… (o genérica, ver §2) | ACTIVO |
| `DEUDA_COMERCIAL` (CxP) | por **proveedor** (§2) | `2.1.2001`… (o genérica, ver §2) | PASIVO |
| `COSTO_GASTO` | `CATEGORIA` (categoría contable de la factura de compra) | `5.1.2002` Costo Programador, `5.3.2002` Honorarios, … | RN |
| `PERCEPCION_IVA_SUFRIDA` | — | `1.1.2007` Percepcion de IVA a computar | ACTIVO |
| `PERCEPCION_IIBB_SUFRIDA` | `JURISDICCION` (opcional) | `1.1.2008` IIBB SIRCREB a favor | ACTIVO |
| `RETENCION_GANANCIAS_SUFRIDA` | — | `1.1.2011` Anticipo Imp. a las Ganancias | ACTIVO |
| `RETENCION_IVA_SUFRIDA` | — | `1.1.2007` Percepcion de IVA a computar (o cuenta propia, checkpoint) | ACTIVO |
| `DIF_CAMBIO_GANADA` | — | **falta** (ver §3) — propuesto `6.4005` | RP |
| `DIF_CAMBIO_PERDIDA` | — | **falta** (ver §3) — propuesto `6.4006` | RN |
| `ANTICIPO_CLIENTE` | — | **falta** (ver §3) — propuesto `2.1.2018` | PASIVO |
| `ANTICIPO_PROVEEDOR` | — | **falta** (ver §3) — propuesto `1.1.2013` | ACTIVO |
| `RETENCION_PRACTICADA_A_DEPOSITAR` | `TIPO_RETENCION` | **falta** (solo si Montanari es agente de retención — checkpoint) | PASIVO |
| `PERCEPCION_PRACTICADA_A_DEPOSITAR` | `TIPO_PERCEPCION` | **falta** (solo si Montanari es agente de percepción — checkpoint) | PASIVO |

**El medio de fondos (banco/caja/MP) NO se resuelve por este mapeo:** viene de la `cuenta_bancaria` que el usuario elige en el cobro/pago, que ya tiene su cuenta contable espejo 1:1 (F3.1 §2.3). Ver §2.3.

---

## 2. Resolución de cuentas "por entidad" (CxC, CxP, fondos)

Tres cuentas dependen de datos de runtime, no de config estática. Decisión de diseño (a validar en checkpoint):

### 2.1 Créditos por Ventas (CxC) — por cliente
Cada `Cliente` lleva un FK **opcional** `cuenta_cxc_id` (migración menor en F4.2). Resolución de `CREDITO_POR_VENTA`:
1. Si el cliente tiene `cuenta_cxc_id` → esa cuenta.
2. Si no → fila por defecto del mapeo (`CREDITO_POR_VENTA`, discriminador NULL) → una cuenta genérica "Deudores por servicios prestados".
3. En todos los casos, la línea lleva `cliente_id` y `proyecto_id` como dimensión analítica: el saldo por cliente/factura se sigue del modelo de documento (factura↔cobro imputación, F4.2/F4.4), **no** de tener una cuenta contable por cliente.

> El seed de F3.3 trae deudores per-cliente (`1.1.2004.01` Valvecchia, etc.). Recomendación: mantenerlos y linkearlos a su cliente vía `cuenta_cxc_id` (opción A, sin migración de datos), **o** consolidar en una sola "Deudores por Ventas" genérica + dimensión cliente (opción B, más escalable para clientes nuevos). **Pregunta de checkpoint #1.** Los casos numéricos de §7 usan la etiqueta genérica "Créditos por Ventas" — el importe no cambia con la decisión.

### 2.2 Deudas Comerciales (CxP) — por proveedor
Idéntico a CxC: `Proveedor.cuenta_cxp_id` opcional → fallback al default `DEUDA_COMERCIAL`. Línea lleva `proveedor_id` + `proyecto_id`.

### 2.3 Medio de fondos — por cuenta bancaria/dinero
El cobro/pago referencia una `cuenta_bancaria` (F2.4, que incluye banco, caja y Mercado Pago vía su enum de tipo). Esa entidad tiene su **cuenta contable espejo 1:1** (F3.1 §2.3 la nombra explícitamente). La línea de fondos usa esa cuenta y setea `cuenta_bancaria_id` como dimensión. La **moneda** de la línea de fondos es la de la cuenta bancaria (Banco Galicia USD → USD).

---

## 3. Cuentas requeridas que **faltan** en el seed F3.3 (gap analysis)

F3.1 §2.4 lista 4 cuentas "requeridas por el motor" que el seed de F3.3 debía crear, pero el Excel no las tenía, así que **no están**. F4.2/F4.3/F4.4 deben crearlas (migración de datos), o se agregan en una migración dedicada previa. Códigos propuestos (encajan en el esquema del seed):

| Concepto | Código propuesto | Nombre | Naturaleza | Rubro |
|---|---|---|---|---|
| `DIF_CAMBIO_GANADA` | `6.4005` | Diferencia de cambio ganada | RP | Otros Ingresos y Egresos |
| `DIF_CAMBIO_PERDIDA` | `6.4006` | Diferencia de cambio perdida | RN | Otros Ingresos y Egresos |
| `ANTICIPO_CLIENTE` | `2.1.2018` | Anticipos de clientes | PASIVO | 5. Otras Deudas |
| `ANTICIPO_PROVEEDOR` | `1.1.2013` | Anticipos a proveedores | ACTIVO | 3. Otros Créditos por ventas |

Se ubican `6.4005/6.4006` bajo la rama `6 "Otros Ingresos y Egresos"` (madre `OTROS_RESULTADOS`), consistente con `6.4002 Intereses Ganados` (RP) y `6.4003 Comisiones bancarias` (RN) ya existentes de signo mixto. **Pregunta de checkpoint #2:** ¿ubicación y nombres correctos para el plan del contador? (F3.1 §2.4 sugería rubros "Resultados Financieros"/"Gastos Financieros"; acá se reusa el rubro de la rama 6 para no crear rubros nuevos).

---

## 4. Generator: Factura de venta (`origen = FACTURA_VENTA`, implementa F4.2)

**Entrada:** factura de venta confirmada con: cliente, proyecto, moneda, TC, base imponible (neto), alícuota/IVA, percepciones/retenciones practicadas (opcional), total.

**Líneas (todas `generada_auto = true`, dimensión `cliente_id`+`proyecto_id`):**

| Lado | Cuenta (concepto) | Importe ARS | Cuándo |
|---|---|---|---|
| **Debe** | `CREDITO_POR_VENTA` (CxC del cliente) | `round2(total × TC)` | siempre |
| Haber | `INGRESO_VENTA` (VENTA u OTRA_VENTA) | `round2(neto × TC)` | siempre (1..N líneas si hay varios conceptos de ingreso) |
| Haber | `IVA_DEBITO_FISCAL` | `round2(iva × TC)` | si la factura tiene IVA (> 0) |
| Haber | `PERCEPCION/RETENCION_PRACTICADA_A_DEPOSITAR` | `round2(percep × TC)` | solo si Montanari es agente (checkpoint #3) |

- **Balance**: `CxC(total) = ingresos(neto) + IVA + percepciones_practicadas`. Por construcción `total = neto + IVA + percep`.
- **Moneda**: cada línea en la moneda de la factura; en USD, `importe_original` = el componente en USD (neto/iva/total), `tipo_cambio` = TC de la factura, `fuente_tc = MANUAL`. No hay diferencia de cambio en la emisión de la factura (§7 sale a cobro).
- **Sin IVA** (exportación / exento): se omite la línea de IVA.

---

## 5. Generator: Factura de compra (`origen = FACTURA_COMPRA`, implementa F4.3)

**Entrada:** proveedor, proyecto (opcional), moneda, TC, categoría contable, neto, IVA crédito fiscal (si corresponde), percepciones sufridas, retenciones, total.

**Líneas (`generada_auto = true`, dimensión `proveedor_id`+`proyecto_id`):**

| Lado | Cuenta (concepto) | Importe ARS | Cuándo |
|---|---|---|---|
| **Debe** | `COSTO_GASTO` (por categoría) | `round2(neto × TC)` | siempre (1..N líneas si la compra se reparte en varias categorías/proyectos) |
| Debe | `IVA_CREDITO_FISCAL` | `round2(iva × TC)` | si el comprobante da crédito fiscal (según condición IVA del proveedor y tipo de comprobante) |
| Debe | `PERCEPCION_IVA_SUFRIDA` / `PERCEPCION_IIBB_SUFRIDA` | `round2(percep × TC)` | si la factura trae percepciones |
| Haber | `DEUDA_COMERCIAL` (CxP del proveedor) | `round2(total × TC)` | siempre |

- **Balance**: `costo(neto) + IVA_CF + percepciones = CxP(total)`.
- **Crédito fiscal condicional**: comprobante tipo C / monotributista / consumidor final → **sin** línea de IVA CF; el IVA se absorbe en el costo (`Debe costo = neto + iva`). El generator recibe del documento la bandera "computa crédito fiscal"; F4.3 la deriva de la condición IVA del proveedor + tipo de comprobante.
- **Retenciones practicadas** (Montanari retiene al proveedor) **no** van en la factura: aparecen en el **pago** (§6).

---

## 6. Generator: Cobro y Pago (`origen = COBRO` / `PAGO`, implementa F4.4)

El cobro/pago imputa a uno o más comprobantes. El asiento tiene la línea de fondos, la línea que cancela el CxC/CxP, la(s) línea(s) de diferencia de cambio (si USD y TC ≠ TC factura) y las de retenciones sufridas/practicadas.

### 6.1 Cobro (cancela una factura de venta)

| Lado | Cuenta | Importe ARS |
|---|---|---|
| **Debe** | Fondos (cuenta bancaria elegida) | `round2(cobrado_orig × TC_cobro)` = lo efectivamente ingresado |
| Debe | `RETENCION_*_SUFRIDA` | `round2(retención × TC_cobro)` (si el cliente retuvo) |
| Debe | `DIF_CAMBIO_PERDIDA` | `|dif|` si `dif < 0` (§6.3) |
| Haber | `CREDITO_POR_VENTA` (CxC) | `round2(imputado_orig × TC_factura)` = lo que cancela del crédito |
| Haber | `DIF_CAMBIO_GANADA` | `dif` si `dif > 0` (§6.3) |

### 6.2 Pago (cancela una factura de compra)

| Lado | Cuenta | Importe ARS |
|---|---|---|
| **Debe** | `DEUDA_COMERCIAL` (CxP) | `round2(imputado_orig × TC_factura)` = lo que cancela de la deuda |
| Debe | `DIF_CAMBIO_PERDIDA` | `|dif|` si `dif > 0` (§6.3, signo invertido vs cobro) |
| Haber | Fondos (cuenta bancaria) | `round2(pagado_orig × TC_pago)` = lo efectivamente egresado |
| Haber | `RETENCION_PRACTICADA_A_DEPOSITAR` | `round2(retención × TC_pago)` (si Montanari retuvo — checkpoint #3) |
| Haber | `DIF_CAMBIO_GANADA` | `dif` si `dif < 0` (§6.3) |

### 6.3 Algoritmo de diferencia de cambio (F3.1 §6.2, central)

```
dif_cambio_ars = round2(imputado_orig × TC_operacion) − round2(imputado_orig × TC_comprobante)
```
- `TC_operacion` = TC del cobro/pago; `TC_comprobante` = TC con que se registró la factura.
- Se calcula **sobre los ARS ya redondeados de cada lado** (no sobre productos crudos): garantiza que las líneas ARS del asiento balanceen exactas por construcción.
- **Signo y cuenta** (F3.1 §6.2):

| Operación | `dif > 0` (TC subió) | `dif < 0` (TC bajó) |
|---|---|---|
| **Cobro** de venta USD | GANANCIA → Haber `DIF_CAMBIO_GANADA` | PÉRDIDA → Debe `DIF_CAMBIO_PERDIDA` |
| **Pago** de compra USD | PÉRDIDA → Debe `DIF_CAMBIO_PERDIDA` | GANANCIA → Haber `DIF_CAMBIO_GANADA` |

- **`dif = 0` ⇒ no se genera línea** de diferencia (regla explícita, CP-07).
- **Comprobante ARS** (§6.4 de F3.1): no hay diferencia de cambio aunque el medio sea USD; el crédito/deuda es fijo en ARS y la línea de fondos debe igualarlo (el usuario ajusta USD o TC).

### 6.4 Regla del residuo (F3.1 §6.3, D-5)

En imputaciones parciales sucesivas de un comprobante USD, cada imputación cancela el CxC/CxP por `round2(imputado_orig × TC_comprobante)`. Redondeos sucesivos pueden dejar un residuo de centavos cuando el saldo en USD ya es 0. **Regla:** la imputación que lleva el saldo **en moneda original** del comprobante a **cero** cancela el CxC/CxP por el **saldo ARS contable remanente exacto** (no por la fórmula), y la diferencia se absorbe en la línea de diferencia de cambio. Garantiza: saldo USD = 0 ⇒ saldo ARS = 0 (nunca quedan créditos de $0,01 imposibles de cerrar). Ver CO-3 (§7).

### 6.5 Anticipos (cobro/pago sin comprobante) e imputación diferida (F3.1 §6.2 caso anticipo, D-4)

- **Cobro anticipo** (sin factura): Debe Fondos `round2(orig × TC_cobro)` / Haber `ANTICIPO_CLIENTE` por el mismo importe. Sin diferencia de cambio (no hay comprobante contra qué medir).
- **Pago anticipo**: Debe `ANTICIPO_PROVEEDOR` / Haber Fondos.
- **Imputación posterior del anticipo a una factura**: el asiento del cobro/pago original **jamás se edita** (está confirmado). Se genera un **asiento nuevo `origen = AJUSTE`** en la fecha de la imputación, que cancela el anticipo contra el CxC/CxP y materializa la diferencia de cambio entre `TC_anticipo` (= `TC_operacion`) y `TC_factura` (= `TC_comprobante`). Ver CO-5 (§7).

---

## 7. Casos de prueba numéricos

TC en formato `1.500,000000`; ARS con 2 decimales HALF_UP. Todos verificados: cada asiento balancea exacto (`Σ debe = Σ haber`). Los CP-06..CP-11 de F3.1 §10 están incluidos y reproducidos al centavo.

### Factura de venta

**FV-1 (=CP-06) — ARS con IVA 21%.** Neto 100.000,00, IVA 21.000,00, total 121.000,00.
`Debe CxC 121.000,00 / Haber Ingresos por ventas 100.000,00 / Haber IVA Débito Fiscal 21.000,00.`

**FV-2 (=CP-07 parte factura) — USD exportación sin IVA.** USD 1.000,00, TC 1.500,000000.
`Debe CxC 1.500.000,00 / Haber Ingresos por ventas 1.500.000,00.` (líneas en USD orig 1.000,00 / TC 1.500)

**FV-3 — ARS "otros ingresos" con IVA.** Neto 50.000,00 (otra venta), IVA 10.500,00, total 60.500,00.
`Debe CxC 60.500,00 / Haber Ingresos por otras ventas 50.000,00 / Haber IVA Débito Fiscal 10.500,00.`

### Factura de compra

**FC-1 — ARS con crédito fiscal.** Categoría "Programador", neto 100.000,00, IVA 21.000,00, total 121.000,00.
`Debe Costo Programador 100.000,00 / Debe IVA Crédito Fiscal 21.000,00 / Haber CxP (proveedor) 121.000,00.`

**FC-2 (=CP-10 parte factura) — USD exterior sin IVA.** USD 200,00, TC 1.500,000000.
`Debe Costo de Prestación de Servicios 300.000,00 / Haber Deudas Comerciales 300.000,00.`

**FC-3 — ARS con percepción de IVA sufrida.** Neto 100.000,00, IVA 21.000,00, percepción IVA 3.000,00, total 124.000,00.
`Debe Costo 100.000,00 / Debe IVA Crédito Fiscal 21.000,00 / Debe Percepcion de IVA a computar 3.000,00 / Haber CxP 124.000,00.`

**FC-4 — comprobante sin crédito fiscal (proveedor monotributo).** Total 50.000,00 (IVA no computable).
`Debe Costo 50.000,00 / Haber CxP 50.000,00.` (el IVA, si viniera discriminado, se absorbe en el costo)

### Cobro

**CO-1 (=CP-07) — USD, mismo TC: sin diferencia.** Factura USD 1.000,00 @1.500 (CxC 1.500.000,00). Cobro USD 1.000,00 @1.500,000000.
`Debe Banco Galicia USD 1.500.000,00 / Haber CxC 1.500.000,00.` `dif = 0 ⇒ sin línea de diferencia.`

**CO-2 (=CP-08) — USD, TC mayor: ganancia.** Factura USD 1.000,00 @1.500. Cobro USD 1.000,00 @1.550,000000.
`Debe Banco USD 1.550.000,00 / Haber CxC 1.500.000,00 / Haber Diferencia de cambio ganada 50.000,00.`

**CO-3 (=CP-09) — USD, dos cobros parciales con TC distinto + regla del residuo.** Factura USD 1.000,00 @1.500 (CxC 1.500.000,00).
- Cobro 1: USD 400,00 @1.520,000000 → `Debe Banco 608.000,00 / Haber CxC 600.000,00 / Haber Dif. ganada 8.000,00.`
- Cobro 2 (cancela saldo): USD 600,00 @1.490,000000 → `Debe Banco 894.000,00 / Debe Dif. perdida 6.000,00 / Haber CxC 900.000,00` (900.000 = saldo ARS remanente exacto, §6.4).
- ✔ CxC saldado en ARS y USD; diferencia neta del ciclo = +2.000,00.

**CO-4 — ARS con retención de Ganancias sufrida.** Factura ARS total 121.000,00; el cliente retiene Ganancias 2% s/neto 100.000,00 = 2.000,00.
`Debe Banco 119.000,00 / Debe Anticipo Imp. a las Ganancias 2.000,00 / Haber CxC 121.000,00.`

**CO-5 (=CP-11) — anticipo USD imputado después (asiento AJUSTE).**
1. Cobro sin factura USD 500,00 @1.500 → `Debe Banco USD 750.000,00 / Haber Anticipos de clientes 750.000,00.`
2. Factura posterior USD 500,00 @1.560 → `Debe CxC 780.000,00 / Haber Ingresos por ventas 780.000,00.`
3. Imputación (asiento nuevo `AJUSTE`, fecha de imputación): `Debe Anticipos de clientes 750.000,00 / Debe Diferencia de cambio perdida 30.000,00 / Haber CxC 780.000,00.`
- ✔ El asiento del cobro (1) no se modifica; factura COBRADA; anticipo saldado.

### Pago

**PA-1 (=CP-10) — USD, TC mayor: pérdida.** Factura compra USD 200,00 @1.500 (CxP 300.000,00). Pago USD 200,00 @1.540,000000.
`Debe Deudas Comerciales 300.000,00 / Debe Diferencia de cambio perdida 8.000,00 / Haber Banco USD 308.000,00.`

**PA-2 — ARS total.** Factura ARS 121.000,00. Pago por transferencia 121.000,00.
`Debe Deudas Comerciales 121.000,00 / Haber Banco Galicia CC 121.000,00.`

**PA-3 — USD parcial, TC menor: ganancia.** Factura compra USD 200,00 @1.500 (CxP 300.000,00). Pago parcial USD 100,00 @1.460,000000.
`Debe Deudas Comerciales 150.000,00 / Haber Banco USD 146.000,00 / Haber Diferencia de cambio ganada 4.000,00.` (saldo CxP restante USD 100,00 = 150.000,00 ARS)

**PA-4 — ARS con retención de Ganancias practicada** (solo si Montanari es agente, checkpoint #3). Factura ARS 121.000,00; retiene Ganancias 2.000,00.
`Debe Deudas Comerciales 121.000,00 / Haber Banco 119.000,00 / Haber Retenciones a depositar 2.000,00.`

---

## 8. Regla de edición post-generación (F3.1 §4.2, funcional §5.2)

El asiento generado es editable después de confirmado, **sin alterar el comprobante origen**:
- Cualquier usuario CARGA/ADMIN puede **agregar pares de líneas manuales balanceadas** (`generada_auto = false`) para ajustes/reclasificaciones (ej.: reclasificar parte de un ingreso a otro rubro). Las líneas `generada_auto = true` quedan intactas.
- Solo ADMIN puede modificar/quitar líneas `generada_auto = true` (F3.1 §4.6). La UI advierte que el asiento divergirá del comprobante.
- Ya implementado en F3.5 (`AsientoService.editarConfirmado`, códigos `SOLO_ADMIN_EDITA_LINEAS_AUTOMATICAS`); F4.2-F4.4 no re-implementan nada de esto.

---

## 9. Contrato de servicio para F4.2-F4.4

```
interface AsientoGenerator<T> { AsientoGenerado generar(T eventoConfirmado); }   // molde PL-4, ya existe

// El documento (FacturaVenta/FacturaCompra/Cobro/Pago), al confirmarse:
//   1. arma su AsientoGenerado con las líneas de §4-§6 (cuentas vía ResolutorCuentas)
//   2. lo entrega a AsientoService.registrarAutomatico(asientoGenerado)  [F3.1 §8.1]
//      -> valida balance (§0.1), numera, confirma, audita, EN LA MISMA TRANSACCIÓN
//   3. persiste el vínculo origen_tipo/origen_id (bidireccional)
// Si el generador o el balance fallan, la confirmación del documento entera hace rollback.
```

- `LineaAsientoGenerada` se amplía a la forma real (F3.1 §8.2): `cuentaCodigo, debe, haber, monedaId, importeOriginal, tipoCambio, fuenteTc, proyectoId?, etapaId?, clienteId?, proveedorId?, cuentaBancariaId?, leyenda?, generadaAuto`.
- `registrarAutomatico` **no existe todavía** en el `AsientoService` de F3.4/F3.5 (que hoy solo tiene `crearBorrador/confirmar/editarConfirmado/duplicar/anular`). **Lo agrega F4.2** como primer consumidor, reutilizando el checklist ya extraído en `validarChecklistDeAsiento`.

---

## 10. Preguntas para el checkpoint del contador

1. **CxC/CxP por cliente/proveedor (§2.1/§2.2):** ¿mantener las cuentas per-cliente/per-tipo del Excel linkeadas a cada entidad (opción A), o consolidar en "Deudores por Ventas" / "Deudas Comerciales" genéricas con el cliente/proveedor como dimensión (opción B)? La contabilidad es la misma; cambia solo dónde vive el detalle por cliente.
2. **Cuentas faltantes (§3):** validar códigos, nombres, naturaleza y ubicación de las 4 cuentas nuevas (dif. de cambio ganada/perdida, anticipos de clientes/proveedores). ¿Van bajo la rama `6`, o preferís rubros "Resultados Financieros"/"Gastos Financieros" separados?
3. **¿Montanari es agente de percepción/retención?** Si no lo es (lo esperable en una empresa de este tamaño), se descartan `PERCEPCION/RETENCION_PRACTICADA_A_DEPOSITAR` y los casos FV-4/PA-4; solo quedan las **sufridas** (en compras y cobros). Confirmar.
4. **Crédito fiscal condicional (§5):** validar la regla de cuándo un comprobante de compra da IVA crédito fiscal (tipo A vs B/C, condición del proveedor) — de esto depende si el IVA va a cuenta propia o se absorbe en el costo.
5. **Retenciones sufridas en el cobro (§6.1, CO-4):** ¿sobre qué base se calcula cada retención (neto/total) y qué cuenta de crédito fiscal usa cada tipo (Ganancias → `1.1.2011`; IVA → ¿`1.1.2007` u otra?; IIBB/SIRCREB → `1.1.2008`)?
6. Repasar los 15 casos numéricos de §7 con calculadora: son los importes exactos que F4.2-F4.4 deberán reproducir.
