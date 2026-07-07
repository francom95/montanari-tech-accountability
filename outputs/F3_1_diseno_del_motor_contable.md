# [F3.1] Diseño del motor contable

> **Estado:** pendiente de checkpoint humano (contador — foco en diferencias de cambio y anulaciones, ver §11).
> **Modelo ejecutor:** Claude Fable 5.
> **Entradas usadas:** `outputs/F1_1_arquitectura_global_y_modelo_de_datos.md` (§3, §5, §6.4, §8, ADRs), `inputs/documento_funcional.md` (§4, §5.6, §11, §15), molde F1.8 (`common/asiento/*`, `common/estado/*`).
> **Alcance:** solo especificación. Implementan sobre este documento: F3.2 (plan de cuentas), F3.4 (motor de asientos), F3.5 (búsqueda/duplicación/anulación), F3.6 (mayores), F4.1 (reglas de asientos automáticos), F11.1 (revisión final). Los casos de prueba de §10 son los tests de aceptación de esos pasos.

---

## 0. Qué hereda de F1.1 y qué decide este documento

F1.1 dejó el modelo de datos cerrado (`cuenta_contable`, `rubro`, `secuencia`, `asiento`, `asiento_linea` — §6.4 y §7 de F1.1) y **tres decisiones abiertas a propósito** (ADR-08, §6.4): la estrategia fina de anulación, la ubicación definitiva de las dimensiones analíticas, y la mecánica exacta de las diferencias de cambio. Este documento cierra esas tres, especifica los algoritmos (validación, numeración, mayores, saldos) y define el set de casos de aceptación.

Ninguna decisión de acá requiere cambios al DER de F1.1. Donde este documento **refina** una regla de F1.1 lo marca explícitamente (hay una sola: desactivación de cuentas con movimientos, §2.4).

---

## 1. Glosario mínimo

- **Asiento**: cabecera (`asiento`) + 2..N líneas (`asiento_linea`). El "número interno compartido por todas las líneas" del funcional §4.3 se satisface estructuralmente: las líneas pertenecen a una cabecera y la cabecera porta el número.
- **Cuenta madre / imputable**: madre agrupa (jerarquía por `padre_id`), imputable recibe líneas. Excluyentes.
- **Confirmado**: único estado que impacta libros, mayores y reportes. Borrador y anulado no existen para ninguna agregación contable.
- **ARS**: moneda de libro (ADR-03). `debe`/`haber` siempre en ARS.

---

## 2. Plan de cuentas

### 2.1 Estructura

| Elemento | Regla |
|---|---|
| Jerarquía | `padre_id` (adjacency list). Profundidad máxima validada: **5 niveles**. Recorridos con CTE recursivo (MySQL 8). |
| Naturaleza | Enum fijo de 5: `ACTIVO, PASIVO, PATRIMONIO_NETO, RESULTADO_POSITIVO, RESULTADO_NEGATIVO`. **No configurable** (define el signo contable y el lado del balance). Lo que el funcional llama "crear nuevas categorías" se satisface con **rubros** (tabla `rubro`: nombre, naturaleza, orden — CRUD de admin) y con cuentas nuevas en cualquier nivel. |
| Código | `VARCHAR(20)`, `UK(tenant_id, codigo)`. Formato libre (el plan real llega del Excel en F3.3, no se le impone disciplina de prefijos). Recomendación para el seed: segmentos numéricos con punto (`1`, `1.1`, `1.1.01`), cero-padded para que el orden natural del string coincida con el orden contable. El código es **inmutable después de que la cuenta tenga movimientos** (es la identidad externa: mayores, exportaciones, migración). |
| Rubro | FK a `rubro`, obligatorio en cuentas imputables, opcional en madres (una madre puede agrupar más arriba que el rubro). |
| Saldo esperado | `DEUDOR / ACREEDOR`. Es **informativo, nunca bloqueante**: (a) etiqueta el saldo del mayor, (b) dispara advertencia/alerta si el saldo confirmado queda contrario al esperado (una cuenta bancaria puede girar en descubierto; bloquear sería incorrecto). Ver CP-20. |
| Proyectos de uso habitual | N:M `cuenta_contable_proyecto` (F1.1 §7). Solo asiste al combo/sugerencias de la UI; no restringe imputación. |

### 2.2 Invariantes (validadas en `CuentaContableService`, error 422 salvo indicación)

1. **Madre ⇒ no imputable**: al crear la primera hija de una cuenta imputable **sin movimientos**, la cuenta pasa automáticamente a no imputable (con confirmación en UI). Si la cuenta ya tiene movimientos, **no puede recibir hijas** (código `CUENTA_CON_MOVIMIENTOS`): crear una hermana y reclasificar con asiento manual es el camino correcto — el sistema no reescribe historia.
2. **Naturaleza coherente**: la naturaleza de una hija debe coincidir con la de su madre. El rubro de una imputable debe tener la misma naturaleza que la cuenta.
3. **Imputable sin hijas**: recíproco de (1); una cuenta con hijas jamás vuelve a ser imputable.
4. **Solo imputables en líneas**: FK validada en service (`CUENTA_NO_IMPUTABLE`) — defensa además del combo de UI que nunca ofrece madres.
5. **Cuenta inactiva**: no puede recibir líneas nuevas ni usarse al confirmar (`CUENTA_INACTIVA`); las líneas históricas quedan intactas y visibles.

### 2.3 Ciclo de vida de cuentas

| Operación | Regla |
|---|---|
| Crear | Libre (respetando invariantes). |
| Editar nombre/rubro/saldo esperado/proyectos | Libre, auditada (`EDITAR`). Naturaleza y código: solo si la cuenta no tiene movimientos. |
| **Desactivar** | **Permitido aunque tenga movimientos** (refinamiento sobre F1.1 §6.4, que lo prohibía — ver §12, D-6). Una cuenta desactivada deja de ofrecerse para nuevas imputaciones; su historia y sus mayores no cambian. Excepciones donde se bloquea: (a) cuenta contable espejo 1:1 de una `cuenta_financiera`/`tarjeta_credito` activa; (b) madre con hijas activas (primero las hijas, sin cascada — la cascada esconde errores). |
| Reactivar | Libre, auditada. |
| **Eliminar** (físico) | Solo sin movimientos, sin hijas y sin vínculo 1:1 financiero. Con movimientos: 409 `TIENE_MOVIMIENTOS_ASOCIADOS`. |

### 2.4 Cuentas requeridas por el motor (seed mínimo)

El motor referencia por función (no por código hardcodeado — resolución vía la tabla de mapeo concepto→cuenta que define F4.1; el seed de F3.3 crea las filas):

| Función | Cuenta (nombre propuesto) | Naturaleza / Rubro |
|---|---|---|
| `DIF_CAMBIO_GANADA` | Diferencia de cambio ganada | RESULTADO_POSITIVO / Resultados Financieros |
| `DIF_CAMBIO_PERDIDA` | Diferencia de cambio perdida | RESULTADO_NEGATIVO / Gastos Financieros |
| `ANTICIPOS_CLIENTES` | Anticipos de clientes | PASIVO / Otras Deudas |
| `ANTICIPOS_PROVEEDORES` | Anticipos a proveedores | ACTIVO / Otros Créditos |

El rubro "Resultados Financieros" se agrega al listado del funcional §4.2 (la lista es extensible por diseño).

---

## 3. Asientos: estructura y decisión de dimensiones

### 3.1 Cabecera vs. línea (decisión pedida por el spec)

**Decisión: las dimensiones analíticas van por LÍNEA** (`proyecto_id`, `etapa_id`, `cliente_id`, `proveedor_id`, `cuenta_financiera_id` como destino de fondos — todas opcionales y NULL por defecto). La cabecera lleva lo que es del asiento como operación: `fecha`, `descripcion` (leyenda general), `estado`, `numero`, `origen`, `origen_tipo/origen_id`, `observaciones`. Cada línea puede además llevar `leyenda` propia.

**Justificación:**
1. Un cobro que cancela dos facturas de dos proyectos distintos es UN asiento con líneas imputadas a proyectos distintos. Lo mismo una factura de compra repartida entre proyectos (F1.1 §6.5 ya lo permite por línea de factura). Dimensiones en cabecera obligarían a partir asientos artificialmente, rompiendo la unidad de la operación.
2. Los filtros de mayores del funcional §4.5 (por proyecto/cliente/proveedor) operan sobre líneas; con dimensión en cabecera, una línea sin relación con el proyecto "contaminaría" el mayor filtrado.
3. El costo de UX se resuelve en la UI, no en el modelo: el editor de asientos ofrece valores a nivel formulario que se **copian a todas las líneas** al cargarlos ("aplicar a todas"), puro azúcar de interfaz — no se persiste nada en cabecera.

`origen` queda en cabecera: el asiento como un todo proviene de un documento (o es manual); una línea no tiene origen propio. `generada_auto BOOLEAN` es de línea: distingue, dentro de un asiento automático, las líneas del generador de las agregadas a mano (funcional §5.2).

### 3.2 Numeración interna

- `numero` es **NULL en borrador** y se asigna al confirmar: correlativo por tenant desde la tabla `secuencia` (fila `ASIENTO`) leída con **lock pesimista** (`SELECT ... FOR UPDATE`) dentro de la transacción de confirmación (ADR-06). Sin huecos por borradores descartados; sin race conditions.
- El número es **inmutable** una vez asignado; nunca se reusa (ni siquiera el de un asiento anulado).
- El orden de los números refleja el orden de **confirmación**, no la fecha contable (los reportes ordenan por fecha; el número es identidad y trazabilidad, no cronología — funcional §4.3). Ver CP-04.

### 3.3 Reglas de línea

- `CHECK (debe >= 0 AND haber >= 0 AND (debe = 0) <> (haber = 0))` — exactamente uno de los dos lados por línea (`LINEA_DEBE_XOR_HABER` en service, CHECK en BD como defensa).
- Mínimo **2 líneas** para confirmar (`ASIENTO_SIN_LINEAS` si 0; `ASIENTO_INCOMPLETO` si 1). Un borrador puede tener 0..N.
- Multimoneda por línea: `moneda_id`, `importe_original DECIMAL(18,2)`, `tipo_cambio DECIMAL(18,6)`, `fuente_tc`. Para líneas ARS: `tipo_cambio = 1.000000`, `importe_original = debe+haber`.
- **Consistencia ARS**: al confirmar se valida `debe+haber = round2(importe_original × tipo_cambio)` (HALF_UP) por línea (`MONTO_ARS_INCONSISTENTE`). El usuario nunca tipea el ARS de una línea en moneda extranjera: carga original + TC y el sistema materializa el ARS. Si necesita otro ARS, ajusta el TC (`fuente_tc = MANUAL`).

### 3.4 Validaciones al confirmar (checklist completa, en orden)

1. Estado actual permite la transición (PL-5, `TransicionEstadoValidator`).
2. ≥ 2 líneas; cada línea XOR debe/haber; cuentas imputables y activas.
3. Consistencia ARS por línea (§3.3). Si alguna línea en moneda extranjera no tiene TC y no hay cotización cargada para (moneda, fecha, fuente default): `TC_FALTANTE` — no se adivina (F1.1 §3).
4. **Balanceo: Σ debe = Σ haber en ARS, igualdad exacta** (`BigDecimal.compareTo`, sin tolerancia — el sistema calculó ambos lados, no hay derecho a diferencia). En moneda original NO se exige balanceo (una línea de diferencia de cambio existe solo en ARS). `ASIENTO_NO_BALANCEA`.
5. `PeriodoGuard`: la fecha del asiento no cae en período cerrado (o admin + motivo, §8).
6. Asignación de número (§3.2) + `confirmado_en/por` + auditoría `CONFIRMAR`.

Todo en una transacción; `AsientoService` es el **único punto de escritura** de `asiento`/`asiento_linea` en el sistema (ADR-07): los generadores automáticos de F4.x construyen un `AsientoGenerado` y se lo entregan a `AsientoService`, jamás insertan directo.

### 3.5 Borradores

- Pueden estar desbalanceados, incompletos, sin TC, con fecha en período cerrado. Ninguna validación contable aplica hasta confirmar (funcional §4.3: "podría permitirse guardarlo como borrador").
- Se editan y **eliminan físicamente** sin restricción de período (un borrador nunca impactó libros; no hay nada que proteger). Única regla: auditoría `ELIMINAR` si el borrador provenía de duplicación de un confirmado (trazabilidad de intención).
- No aparecen en mayores, reportes ni saldos. Aparecen en la búsqueda/listado de asientos con su estado visible.

---

## 4. Ciclo de vida

### 4.1 Estados y transiciones

PL-5 estricta (molde F1.8 ya scaffoldeado): `BORRADOR → CONFIRMADO → ANULADO`; `ANULADO` terminal; borrador eliminable físicamente. Confirmar un documento (factura/cobro/pago/liquidación) genera y confirma su asiento **en la misma transacción** (F1.1 §8).

### 4.2 Edición de asientos confirmados

Permitida con auditoría `EDITAR` (datos antes/después en JSON del DTO completo del asiento):

| Qué se puede editar | Quién | Condición |
|---|---|---|
| Descripción, observaciones, leyendas de línea, dimensiones analíticas | CARGA, ADMIN | — |
| Fecha | CARGA, ADMIN | `PeriodoGuard` sobre fecha vieja **y** nueva (§8) |
| Agregar/quitar/modificar líneas **manuales** (`generada_auto = false`) | CARGA, ADMIN | Rebalanceo: el asiento debe volver a pasar §3.4 completo |
| Modificar/quitar líneas **`generada_auto = true`** | **solo ADMIN** | Funcional §14.1 ("editar asientos automáticos" es facultad del administrador). La UI advierte que el asiento divergirá del comprobante; auditoría marca `detalle = "edición de líneas autogeneradas"` |
| Número, origen, origen_tipo/id | nadie | Inmutables |

El caso de uso del funcional §5.2 —"agregar cuentas que no afecten a la factura"— es la fila 3: cualquier usuario de carga suma pares de líneas manuales balanceadas a un asiento automático sin tocar las líneas generadas ni el comprobante.

### 4.3 Duplicación

Duplicar un asiento (cualquier estado) crea un **BORRADOR nuevo**: copia líneas, cuentas, importes, monedas, TC y dimensiones; `numero = NULL`; `fecha = hoy` (editable); **`origen = MANUAL` siempre** y sin vínculo `origen_tipo/origen_id` (el duplicado es una operación nueva del usuario, aunque la fuente fuera automática — duplicar una factura se hace desde facturación, no desde su asiento). Auditoría `DUPLICAR` sobre el asiento fuente con id del nuevo en `detalle`.

### 4.4 Anulación — decisión contra-asiento vs. marca

**Decisión: híbrido determinado por el estado del período de la fecha original.** Es la única combinación que cumple a la vez "anulado conserva trazabilidad" (funcional §5.7) y "lo declarado no muta" (ADR-17):

| Situación | Mecanismo | Efecto |
|---|---|---|
| **Fecha del asiento en período ABIERTO** | **Marca**: `estado → ANULADO`, `motivo_anulacion` obligatorio, `anulado_en/por` | El asiento queda visible con su número, excluido de todo cálculo. El mes aún no fue usado para liquidar: puede cambiar. |
| **Fecha del asiento en período CERRADO** | **Contra-asiento**: se genera un asiento nuevo `origen = AJUSTE` con las líneas espejadas (debe↔haber, mismas cuentas, dimensiones y moneda/TC originales), `fecha = hoy` (o la que elija el usuario, siempre en período abierto), y se enlaza `original.asiento_anulacion_id = contraasiento.id`. **El original permanece `CONFIRMADO`**: su mes cerrado no cambia ni un centavo; la reversión impacta el mes abierto. La UI muestra el original como "Revertido por asiento N° X". | Los reportes del mes cerrado quedan idénticos a lo declarado; el neto histórico-acumulado es cero. |

Reglas complementarias:

- La anulación por marca de un asiento en período cerrado **existe pero es excepcional**: solo ADMIN + motivo + auditoría reforzada (`sobre_periodo_cerrado = TRUE`), porque sí altera reportes de un mes declarado (ej.: corregir un error material antes de rectificar impuestos). El camino normal en cerrado es el contra-asiento, disponible para CARGA (no modifica el período cerrado: el original solo recibe el link, sin efecto contable).
- **Asientos de documentos se anulan desde el documento**: si `origen ∈ {FACTURA_VENTA, FACTURA_COMPRA, COBRO, PAGO, LIQUIDACION_IVA, LIQUIDACION_IIBB, RESUMEN_TARJETA, MOVIMIENTO_BANCARIO}`, la anulación directa del asiento se rechaza (`ANULACION_VIA_DOCUMENTO`): anular la factura anula/revierte su asiento con la misma regla de período, y el documento pasa a `ANULADO`. Evita divergencia documento↔asiento. Orígenes `MANUAL`, `AJUSTE`, `APERTURA`, `IMPORTACION` se anulan directo.
- Anular un documento con imputaciones (factura con cobros aplicados) exige primero anular/desimputar los cobros: `TIENE_MOVIMIENTOS_ASOCIADOS` (la regla fina de desimputación es de F4.4; el motor solo garantiza que no queden imputaciones colgando de documentos anulados).
- Un contra-asiento no es anulable por marca (anularía la reversión dejando al original revertido-pero-no): se revierte con otro contra-asiento si hiciera falta (caso rarísimo, soportado sin regla especial).

### 4.5 Eliminación física

Solo borradores (§3.5). Los confirmados **nunca** se eliminan físicamente: el "eliminarse según permisos" del funcional §4.4 se interpreta como eliminación de borradores + anulación de confirmados (la trazabilidad del §5.7 es incompatible con borrar confirmados; validar con el equipo en checkpoint).

### 4.6 Matriz de permisos

| Operación | LECTURA | CARGA | ADMINISTRADOR |
|---|---|---|---|
| Ver, buscar, exportar asientos y mayores | ✓ | ✓ | ✓ |
| Crear/editar/eliminar borradores | — | ✓ | ✓ |
| Confirmar (fecha en período abierto) | — | ✓ | ✓ |
| Editar confirmado: campos y líneas manuales | — | ✓ | ✓ |
| Editar/quitar líneas `generada_auto` | — | — | ✓ |
| Duplicar | — | ✓ | ✓ |
| Anular por marca (período abierto) / contra-asiento | — | ✓ | ✓ |
| Cualquier escritura con fecha en período cerrado (incl. anulación por marca en cerrado) | — | — | ✓ + motivo + auditoría reforzada |
| Cerrar / reabrir período | — | — | ✓ |

---

## 5. Mayores y saldos

### 5.1 Principio

**Todo saldo es derivado por consulta** sobre `asiento_linea` JOIN `asiento` con `estado = CONFIRMADO` (ADR-04). No existen columnas de saldo acumulado. El único "saldo persistido" del sistema son los saldos iniciales de cuentas financieras, que se materializan como asiento de `APERTURA` (F10.3) y por lo tanto también entran al mayor por la vía normal.

### 5.2 Mayor de una cuenta imputable

- **Orden**: `(asiento.fecha ASC, asiento.numero ASC, linea.orden ASC)`. La fecha manda (funcional §4.3); el número desempata de forma estable dentro del día.
- **Columnas**: fecha, número de asiento, descripción (leyenda de línea si existe, si no la de cabecera), debe, haber, **saldo acumulado** (running sum de `debe − haber` desde el inicio del filtro), y al pie **saldo final** con etiqueta **DEUDOR** si `Σdebe − Σhaber > 0`, **ACREEDOR** si `< 0`, **SALDADA** si `= 0`. El signo es uniforme para toda naturaleza (el criterio deudor/acreedor es universal); `saldo_esperado` solo agrega la advertencia visual si el resultado es contrario.
- **Filtro por rango de fechas**: la primera fila es **"Saldo anterior"** = acumulado de todo lo confirmado antes de `fecha_desde` (calculado en la misma consulta). Ver CP-17.
- **Moneda original**: cada fila expone además `moneda`, `importe_original`, `tipo_cambio` — para cuentas en USD habilita la lectura "evolución en moneda original" (F1.1 §3) sin cálculo extra.

### 5.3 Mayor de una cuenta madre

Agregación de todas las imputables descendientes (CTE recursivo). Mismas columnas; cada fila indica la cuenta imputable concreta. El saldo de una madre = Σ saldos de descendientes.

### 5.4 Filtros analíticos (funcional §4.5)

Fecha/período, cuenta, rubro, proyecto, cliente, proveedor, tipo de operación (= `asiento.origen`), moneda (= `linea.moneda_id`). Cuando se filtra por dimensión analítica, el mayor resultante es una **vista analítica**: el saldo acumulado corre sobre el subconjunto filtrado y se rotula "saldo del filtro" (no es el saldo contable de la cuenta — evita interpretaciones erróneas; el saldo contable solo existe sin filtros analíticos).

### 5.5 Balance de sumas y saldos (insumo de F7.2)

Por cuenta imputable (y agregable por madre/rubro/naturaleza): `total_debe`, `total_haber`, `saldo_deudor = max(debe−haber, 0)`, `saldo_acreedor = max(haber−debe, 0)`. Invariantes verificables (job nocturno de integridad + F11.1): `Σ total_debe = Σ total_haber` global, y `Σ saldos deudores = Σ saldos acreedores`. Ver CP-18.

### 5.6 Interacción con períodos

Ninguna para lectura: los asientos de períodos cerrados **siempre** se consultan, exportan y suman con normalidad (funcional §15 — el cierre restringe escritura, jamás lectura). No existe "recálculo por cierre": cerrar un período no materializa nada.

---

## 6. Multimoneda dentro del asiento

### 6.1 Convivencia original / TC / ARS

Patrón `ImporteMonetario` de F1.1 §3 aplicado a nivel línea (§3.3 de este documento). Reglas:

- La contabilidad balancea **solo en ARS**. Un asiento puede mezclar líneas ARS y USD; la suma de `importe_original` por moneda no tiene por qué balancear (una línea de diferencia de cambio es un hecho puramente ARS: `moneda = ARS`, `importe_original = importe ARS`).
- Redondeo: **cada materialización a ARS redondea HALF_UP a 2 decimales en ese momento** (`round2(original × TC)`); nunca se arrastran más decimales. TC con 6 decimales máximo. Centralizado en `common/money` (F1.1 §1.4).
- Fuente de TC: default por parámetro de sistema (`FUENTE_TC_DEFAULT`), pisable por operación (`MANUAL`). Sin cotización disponible al confirmar → `TC_FALTANTE` (nunca se adivina ni se toma "la última").

### 6.2 Diferencia de cambio: cuándo y cómo

**Criterio: percibido.** La diferencia de cambio se reconoce **únicamente al imputar un cobro/pago** a un comprobante en moneda extranjera. No hay revaluación de tenencia (ver §6.5).

**Fórmula** (por imputación):

```
dif_cambio_ars = round2(importe_imputado_original × TC_operacion)
               − round2(importe_imputado_original × TC_comprobante)
```

- `TC_operacion` = TC del cobro/pago; `TC_comprobante` = TC con que se registró la factura.
- Se calcula **sobre los ARS ya redondeados** de cada lado (no se redondea la diferencia de productos crudos): así las líneas del asiento — que son ARS redondeados — balancean exactas por construcción.
- Se persiste en `cobro_imputacion.dif_cambio_ars` / `pago_imputacion.dif_cambio_ars` (con signo) para el reporte por proyecto, además de materializarse en líneas.

**Signo y cuenta** (la cuenta se resuelve por el mapeo concepto→cuenta de F4.1, funciones §2.4):

| Operación | `dif > 0` (TC subió) | `dif < 0` (TC bajó) |
|---|---|---|
| Cobro de factura de **venta** USD | GANANCIA → Haber "Dif. de cambio ganada" | PÉRDIDA → Debe "Dif. de cambio perdida" |
| Pago de factura de **compra** USD | PÉRDIDA → Debe "Dif. de cambio perdida" | GANANCIA → Haber "Dif. de cambio ganada" |

**Dónde se materializa:**

- **Caso normal** (la imputación nace junto con el cobro/pago): líneas adicionales **dentro del asiento del cobro/pago** (F1.1 §3). Forma canónica del asiento de cobro USD: Debe Banco `round2(cobrado × TC_cobro)` / Haber Créditos por Ventas `round2(imputado × TC_factura)` / línea de dif. de cambio por la diferencia.
- **Caso anticipo** (la imputación se crea después, sobre un cobro/pago ya confirmado): **jamás se edita el asiento confirmado del cobro**. La imputación genera un **asiento nuevo `origen = AJUSTE`** en la fecha de la imputación: cancela anticipo contra crédito/deuda y materializa la diferencia. Ver CP-11. (Refina F1.1 §3, que solo contemplaba el caso normal; principio rector: ningún proceso automático muta un asiento confirmado.)

### 6.3 Regla del residuo (cierre por centavos)

En imputaciones parciales sucesivas, el haber/debe contra la cuenta de créditos/deudas de cada imputación es `round2(imputado_original × TC_comprobante)`. Redondeos sucesivos pueden dejar al crédito con un residuo de centavos cuando el saldo en moneda original ya es cero. Regla: **la imputación que lleva el saldo en moneda original del comprobante a cero cancela el crédito/deuda por el saldo ARS contable remanente exacto** (no por la fórmula), y la diferencia de centavos se absorbe en la línea de diferencia de cambio. Garantiza: saldo USD = 0 ⇒ saldo ARS = 0 (nunca quedan créditos de $0,01 imposibles de cerrar).

### 6.4 Comprobantes ARS

Si el comprobante es ARS, **no existe diferencia de cambio** aunque el medio de cobro sea una cuenta USD: el crédito es fijo en ARS; el cobro debita el banco USD por `round2(USD_recibidos × TC_cobro)` que debe igualar el ARS imputado (el usuario ajusta USD o TC). Cualquier quita/exceso real es imputación parcial o ajuste manual, no dif. de cambio.

### 6.5 Sin revaluación de tenencia (decisión a validar con contador)

V1 **no** revalúa al cierre de mes los saldos de cuentas en moneda extranjera (Banco USD, créditos/deudas USD abiertos). Motivos: es contabilidad de gestión interna (no balance legal), el criterio percibido es el del Excel actual, y la revaluación automática exige política contable que hoy no existe. Si el contador la requiere, el camino ya existe sin cambio de modelo: asiento manual/`AJUSTE` mensual contra las cuentas de diferencia de cambio. **Punto explícito del checkpoint (§11.3).**

---

## 7. Interacción con períodos (matriz de guardas)

`PeriodoGuard` (F1.1 §5) se invoca con la **fecha contable** de la operación. `A` = período abierto, `C` = cerrado:

| Operación | A | C |
|---|---|---|
| Crear/editar/eliminar **borrador** | ✓ | ✓ (exento: un borrador no impacta libros) |
| **Confirmar** asiento/documento | ✓ | ADMIN + motivo + auditoría reforzada |
| **Editar** confirmado | ✓ | ADMIN + motivo + reforzada (guarda sobre fecha original **y** nueva si cambia) |
| **Anular por marca** | ✓ | ADMIN + motivo + reforzada (§4.4) |
| **Contra-asiento de reversión** (fecha del contra-asiento en abierto) | ✓ | n/a — es la vía normal para revertir efectos de un mes cerrado sin tocarlo |
| Consultar / exportar / mayores / reportes | ✓ | ✓ (siempre) |
| Importar a bandeja de revisión | ✓ | ✓ (no impacta contabilidad hasta confirmar) |

---

## 8. Contratos de servicio y evolución del molde F1.8

### 8.1 `AsientoService` (superficie pública — implementa F3.4)

```
crearBorrador(AsientoRequest) → Asiento            // sin validaciones contables
editarBorrador(id, AsientoRequest) → Asiento
eliminarBorrador(id)
confirmar(id | AsientoRequest directo) → Asiento    // checklist §3.4 completa, asigna número
editarConfirmado(id, AsientoEdicionRequest) → Asiento  // §4.2, revalida §3.4
duplicar(id) → Asiento (borrador)                   // §4.3
anular(id, motivo) → Asiento                        // decide marca vs contra-asiento según período (§4.4)
registrarAutomatico(AsientoGenerado) → Asiento      // único punto de entrada de F4.x: valida y confirma en la transacción del documento
```

Códigos de error del motor (se suman al catálogo F1.1 §1.3): `ASIENTO_NO_BALANCEA`, `ASIENTO_SIN_LINEAS`, `ASIENTO_INCOMPLETO`, `LINEA_DEBE_XOR_HABER`, `CUENTA_NO_IMPUTABLE`, `CUENTA_INACTIVA`, `CUENTA_CON_MOVIMIENTOS`, `MONTO_ARS_INCONSISTENTE`, `TC_FALTANTE`, `PERIODO_CERRADO`, `TRANSICION_ESTADO_INVALIDA`, `ANULACION_VIA_DOCUMENTO`, `TIENE_MOVIMIENTOS_ASOCIADOS`.

### 8.2 Evolución del molde F1.8 (breaking esperado, sin código productivo afectado)

`LineaAsientoGenerada` (record del molde) se amplía en F3.4 a la forma real de línea: `cuentaCodigo, debe, haber, monedaId, importeOriginal, tipoCambio, fuenteTc, proyectoId?, etapaId?, clienteId?, proveedorId?, cuentaFinancieraId?, leyenda?`. `ValidadorBalanceAsiento` y `TransicionEstadoValidator` quedan como están (ya expresan §3.4-4 y PL-5). `NumeradorAsientoEnMemoria` se reemplaza por la implementación respaldada en `secuencia` con lock (§3.2); la interfaz `NumeradorAsiento` no cambia.

### 8.3 Job de integridad (defensa en profundidad, ADR-07)

Verificación nocturna + bajo demanda (F11.1): (a) todo asiento `CONFIRMADO` balancea; (b) invariantes de §5.5; (c) ningún `CONFIRMADO` con número NULL ni duplicado; (d) ninguna línea sobre cuenta no imputable; (e) todo documento `CONFIRMADO` con `asiento_id` apunta a asiento `CONFIRMADO` (o revertido por contra-asiento si el documento está `ANULADO`). Reporta a alerta `CRITICA`, nunca "auto-repara".

---

## 9. Decisiones registradas en este paso

| # | Decisión | Alternativa descartada y por qué |
|---|---|---|
| D-1 | Dimensiones analíticas **por línea**; cabecera solo datos de la operación; "aplicar a todas" como azúcar de UI | Dimensiones en cabecera: obliga a partir operaciones multi-proyecto en varios asientos y degrada los filtros de mayores |
| D-2 | Anulación **híbrida por estado del período**: marca en abierto, contra-asiento en cerrado; original de mes cerrado nunca cambia | Solo-marca: mutaría reportes de meses declarados. Solo-contraasiento: ensucia meses abiertos con pares reversos innecesarios y duplica el volumen del diario |
| D-3 | Asientos de documentos se anulan **solo vía documento** (`ANULACION_VIA_DOCUMENTO`) | Anulación directa del asiento: documento vivo apuntando a asiento anulado = libros y comprobantes divergentes |
| D-4 | Dif. de cambio **a lo percibido**, materializada en el asiento del cobro/pago; para anticipos, **asiento `AJUSTE` nuevo** en la fecha de imputación | Editar el asiento confirmado del cobro (mutación automática de confirmados, prohibida); revaluación mensual de tenencia (política contable inexistente, ver §6.5) |
| D-5 | Regla del residuo: la imputación que cierra el saldo en moneda original cancela el ARS remanente exacto contra dif. de cambio | Cancelar siempre por fórmula: deja créditos/deudas de centavos imposibles de cerrar |
| D-6 | Desactivar cuentas **con** movimientos permitido (refina F1.1); eliminar físico solo sin movimientos | Prohibición total de F1.1: obligaría a mantener activas cuentas en desuso, contaminando combos, sin ganancia de integridad (la historia no se toca en ningún caso) |
| D-7 | Confirmados jamás se eliminan físicamente; "eliminar" del funcional = borradores | Borrado físico con permisos: incompatible con trazabilidad §5.7 y con la numeración sin huecos |
| D-8 | Duplicado nace `MANUAL`, sin vínculo a documento origen | Heredar origen: un asiento "FACTURA_VENTA" sin factura real rompe la navegación documento↔asiento (ADR-09) |
| D-9 | Saldo deudor/acreedor por signo uniforme de `Σdebe − Σhaber`; `saldo_esperado` solo advierte | Signo por naturaleza: complica el mayor sin ganancia; bloquear por saldo contrario: falso en la práctica (descubiertos, anticipos) |

---

## 10. Casos de prueba contables (aceptación para F3.4, F4.x, F11.1)

Convenciones: TC en formato `1.500,000000`; importes ARS con 2 decimales; cuentas del seed §2.4 y rubros del funcional §4.2. "confirmar" implica pasar la checklist §3.4. Salvo indicación, el período de la fecha está ABIERTO. Los casos CP-06..CP-11 asumen las reglas de generación automática de F4.1 en su forma mínima (las cuentas exactas del mapeo pueden ajustarse en F4.1 sin invalidar los importes esperados).

**CP-01 — Asiento manual balanceado ARS.**
Asiento 05/06: Debe "Honorarios Profesionales" 50.000,00 / Haber "Banco Galicia CC ARS" 50.000,00.
✔ Confirma; recibe número correlativo; ambas líneas TC 1,000000; auditoría `CONFIRMAR`.

**CP-02 — Desbalanceado: rechazo y borrador.**
Debe 50.000,00 / Haber 45.000,00. ✔ `confirmar` → 422 `ASIENTO_NO_BALANCEA`. ✔ `crearBorrador` con las mismas líneas → OK, `numero = NULL`, invisible en mayores.

**CP-03 — Cuenta madre y línea inválida.**
(a) Línea sobre "Caja y Bancos" (madre) → `CUENTA_NO_IMPUTABLE`. (b) Línea con debe 100,00 **y** haber 100,00 → `LINEA_DEBE_XOR_HABER`. (c) Asiento de 1 línea → `ASIENTO_INCOMPLETO`.

**CP-04 — Numeración por orden de confirmación, sin huecos.**
Se crean borradores A, B, C (en ese orden). Se confirma B → N° n; se elimina C; se confirma A → N° n+1.
✔ C no consumió número; no hay huecos; el orden de números no sigue el orden de creación ni la fecha.

**CP-05 — Fecha intermedia (funcional §4.3).**
Existen confirmados 10/06 y 20/06. Se confirma un asiento 16/06.
✔ Sin error; el mayor y el diario lo ordenan entre ambos por fecha, aunque su número sea el mayor.

**CP-06 — Factura de venta ARS con IVA 21% (asiento automático).**
Factura B: neto 100.000,00, IVA 21.000,00, total 121.000,00, fecha 15/06.
✔ Asiento `origen = FACTURA_VENTA`: Debe "Créditos por Ventas" 121.000,00 / Haber "Ingresos por Ventas" 100.000,00 + Haber "IVA Débito Fiscal" 21.000,00. Líneas `generada_auto = true`; vínculo bidireccional factura↔asiento.

**CP-07 — Venta USD cobrada al mismo TC: sin diferencia.**
Factura E (exportación, sin IVA) USD 1.000,00, TC 1.500,000000 → asiento: Debe Créditos 1.500.000,00 / Haber Ventas 1.500.000,00. Cobro USD 1.000,00 mismo día, TC 1.500,000000, a "Banco Galicia USD".
✔ Asiento cobro: Debe Banco USD 1.500.000,00 / Haber Créditos 1.500.000,00. `dif_cambio_ars = 0,00`; **no** se genera línea de diferencia (regla: dif 0 ⇒ sin línea).

**CP-08 — Venta USD cobrada a TC mayor: ganancia.**
Factura USD 1.000,00 TC 1.500,000000 (crédito 1.500.000,00). Cobro total USD 1.000,00 TC 1.550,000000.
✔ Asiento cobro: Debe Banco USD 1.550.000,00 / Haber Créditos por Ventas 1.500.000,00 / Haber **Dif. de cambio ganada 50.000,00**. `cobro_imputacion.dif_cambio_ars = +50.000,00`. Línea de dif.: moneda ARS, TC 1,000000.

**CP-09 — Venta USD, dos cobros parciales con TC distinto (caso pedido por el spec).**
Factura USD 1.000,00 TC 1.500,000000.
Cobro 1: USD 400,00 TC 1.520,000000 → Debe Banco 608.000,00 / Haber Créditos 600.000,00 / Haber Dif. ganada **8.000,00**.
Cobro 2 (cancela saldo): USD 600,00 TC 1.490,000000 → Debe Banco 894.000,00 + Debe Dif. perdida **6.000,00** / Haber Créditos 900.000,00 (residuo ARS exacto, §6.3).
✔ Crédito queda SALDADO en ARS y en USD; dif. neta del ciclo = +2.000,00; CxC muestra la factura COBRADA.

**CP-10 — Pago de compra USD a TC mayor: pérdida.**
Factura compra (exterior, sin IVA) USD 200,00 TC 1.500,000000 → Debe "Costos de Prestación de Servicios" 300.000,00 / Haber "Deudas Comerciales" 300.000,00. Pago USD 200,00 TC 1.540,000000.
✔ Asiento pago: Debe Deudas Comerciales 300.000,00 + Debe **Dif. de cambio perdida 8.000,00** / Haber Banco USD 308.000,00.

**CP-11 — Anticipo en USD imputado después (asiento AJUSTE).**
1) Cobro sin factura: USD 500,00 TC 1.500,000000 → Debe Banco USD 750.000,00 / Haber "Anticipos de clientes" 750.000,00.
2) Factura E posterior: USD 500,00 TC 1.560,000000 → Debe Créditos 780.000,00 / Haber Ventas 780.000,00.
3) Imputación del anticipo a la factura → **asiento nuevo `origen = AJUSTE`** (fecha de la imputación): Debe Anticipos de clientes 750.000,00 + Debe Dif. de cambio perdida 30.000,00 / Haber Créditos por Ventas 780.000,00.
✔ El asiento del cobro (1) no se modificó; factura COBRADA; anticipo saldado.

**CP-12 — Anulación por marca en período abierto.**
Se anula el asiento de CP-01 (junio abierto) con motivo "carga duplicada".
✔ `estado = ANULADO`, conserva número, `motivo_anulacion` persistido; desaparece de mayores y sumas y saldos; auditoría `ANULAR` con snapshot antes/después; intentar editarlo → `TRANSICION_ESTADO_INVALIDA`.

**CP-13 — Anulación de factura confirmada con período cerrado (contra-asiento).**
La factura de CP-06 (15/06) está confirmada; junio se cierra; el 08/07 se anula la factura.
✔ Se genera contra-asiento `origen = AJUSTE` fecha 08/07: Debe Ingresos por Ventas 100.000,00 + Debe IVA Débito Fiscal 21.000,00 / Haber Créditos por Ventas 121.000,00. El asiento original queda `CONFIRMADO` con `asiento_anulacion_id` → contra-asiento; la factura queda `ANULADA`. Los reportes de junio no cambian; julio absorbe la reversión; el acumulado neto de las 3 cuentas es 0.
✔ Ejecutable por rol CARGA sin auditoría reforzada (no se modificó el período cerrado).

**CP-14 — Edición de asiento automático confirmado.**
Al asiento de CP-06, un usuario CARGA agrega dos líneas manuales: Debe "Gastos de Comercialización" 5.000,00 / Haber "Créditos por Ventas" 5.000,00 (bonificación reclasificada).
✔ Permitido (líneas nuevas `generada_auto = false`); asiento revalida balanceo (ahora 126.000,00 = 126.000,00); factura intacta; auditoría `EDITAR` con antes/después.
✔ El mismo usuario intenta modificar la línea de IVA (`generada_auto = true`) → 403 (solo ADMIN). ADMIN lo hace → OK + auditoría con detalle "edición de líneas autogeneradas".

**CP-15 — Duplicación.**
Se duplica el asiento de CP-01.
✔ Nace BORRADOR sin número, fecha hoy, `origen = MANUAL`, líneas/cuentas/importes/dimensiones copiados; auditoría `DUPLICAR` sobre el original. Editarlo y confirmarlo produce un asiento independiente.

**CP-16 — Escritura en período cerrado (guarda).**
Junio cerrado. Usuario CARGA intenta confirmar un asiento con fecha 25/06 → 422 `PERIODO_CERRADO`. ADMIN lo confirma con motivo "ajuste auditoría" → OK; `auditoria_log.sobre_periodo_cerrado = TRUE` y `detalle` = motivo.
✔ El mismo borrador con fecha 25/06 se pudo crear y editar sin restricción (borradores exentos, §7).

**CP-17 — Mayor con saldo anterior y acumulado.**
"Banco Galicia CC ARS": apertura 01/06 Debe 500.000,00; 05/06 Debe 121.000,00; 12/06 Haber 80.000,00; 18/06 Haber 200.000,00.
✔ Mayor sin filtro: acumulados 500.000,00 → 621.000,00 → 541.000,00 → 341.000,00; saldo final **341.000,00 DEUDOR**.
✔ Mayor filtrado desde 10/06: fila inicial "Saldo anterior 621.000,00", luego 12/06 y 18/06, mismo saldo final.

**CP-18 — Sumas y saldos cuadra.**
Con solo CP-06 y CP-08 cargados (3 asientos confirmados): total debe global = total haber global = **3.171.000,00** (121.000 + 1.500.000 + 1.550.000). Con el dataset completo de CP-01..CP-17: `Σ total_debe = Σ total_haber` y `Σ saldos deudores = Σ saldos acreedores` (invariantes §5.5). Anulados y borradores no aportan.

**CP-19 — TC faltante.**
Se intenta confirmar un asiento con línea USD fecha 03/06, sin TC manual y sin cotización cargada para (USD, 03/06, fuente default) → 422 `TC_FALTANTE`. Como borrador se guarda sin error. Cargada la cotización, confirma.

**CP-20 — Saldo contrario al esperado: advierte, no bloquea.**
"Banco Galicia CC ARS" (`saldo_esperado = DEUDOR`, saldo 341.000,00 tras CP-17) recibe pago confirmado por Haber 400.000,00 → saldo −59.000,00 ACREEDOR.
✔ La confirmación **no** se bloquea; el mayor etiqueta ACREEDOR con advertencia visual; se emite alerta (motor F9.1) por saldo contrario al esperado.

---

## 11. Qué validar en el checkpoint humano (contador)

1. **Anulación híbrida (D-2/D-3, CP-12/CP-13):** ¿acepta que un asiento de mes cerrado se revierta con contra-asiento en el mes abierto, quedando el original visible y computado en su mes? ¿Y que la marca directa en cerrado exista solo como excepción de admin?
2. **Diferencia de cambio a lo percibido (D-4, CP-08..CP-11):** fórmula, signos, cuentas propuestas y el asiento `AJUSTE` para anticipos. ¿Coincide con su criterio? ¿Nombres de cuentas correctos para su plan?
3. **Sin revaluación de tenencia (§6.5):** ¿confirma que V1 no revalúe saldos USD al cierre de mes, o exige revaluación mensual? (si la exige: se hará por asiento manual/AJUSTE con su política, el modelo no cambia).
4. **Regla del residuo (D-5, CP-09):** ¿de acuerdo con absorber los centavos de redondeo en diferencia de cambio al cerrar el comprobante?
5. **Saldo esperado solo-advertencia (D-9, CP-20)** y **eliminación física solo de borradores (D-7)** — validación rápida con el equipo.
6. Repasar los 20 casos de §10 con una calculadora: son los números que el sistema deberá reproducir exactos.

---

## 12. Trazabilidad contra las reglas innegociables del paso

| Regla del spec F3.1 | Dónde queda garantizada |
|---|---|
| Σ debe = Σ haber para confirmar; desbalanceado solo borrador | §3.4-4, CP-02 |
| Fechas intermedias sin orden de carga | §3.2, §5.2, CP-05 |
| Asientos automáticos editables post-generación, con auditoría | §4.2, CP-14 |
| Cuentas madre no imputables | §2.2, CP-03 |
| Estados borrador/confirmado/anulado; solo confirmado impacta | §4.1, §5.1, CP-12 |
| Multimoneda: original + TC + ARS, nunca float/double | §3.3, §6.1 (`DECIMAL`/`BigDecimal`, HALF_UP centralizado) |
| Diferencia de cambio contemplada | §6.2–6.4, CP-07..CP-11 |
| Cierre no bloquea consultas/import/export; modificar cerrado = admin + auditoría reforzada | §7, CP-16 |
| Auditoría con antes/después en operaciones sensibles | §3.4-6, §4.2–4.4, todos los CP con verbo de auditoría |
