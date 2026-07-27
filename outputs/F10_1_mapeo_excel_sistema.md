# F10.1 — Mapeo Excel → sistema

Modelo asignado: Sonnet 5 (sin discrepancia con la sesión activa).

**✅ Checkpoint humano aprobado** — el equipo resolvió los 9 puntos abiertos (ver **"Puntos que el equipo debe decidir"** al final, ahora con las respuestas). Este documento es el insumo directo de F10.2.

Insumo: `Contabilidad 2026 (3).xlsx` (22 hojas), analizado hoja por hoja con Python/openpyxl (valor real + `number_format` de cada celda, no el texto renderizado — necesario porque varias hojas tienen códigos corrompidos a fecha por Excel, ver más abajo).

---

## 🔴 Hallazgo urgente (no relacionado con el mapeo)

La hoja **"Proyectos"**, celdas R21:R26, contiene en texto plano: el CUIT de la empresa, una contraseña de una cuenta, y **usuario, contraseña y URL de login del Home Banking de Banco Galicia**. Esto **no migra bajo ninguna circunstancia** (no es dato de negocio) y **debería rotarse** — quedó expuesto en un Excel que ahora circula por este proceso de migración.

---

## Cómo se decodifican los códigos corrompidos (aplica a "Plan de Cuentas" y "Libro Diario")

Ya documentado en memoria de proyecto (`excel-codigos-como-fechas.md`, de F3.3) y **confirmado aquí con el Excel real**: Excel autoconvirtió muchos códigos de cuenta a fecha. Se decodifican de forma determinística por `valor + number_format` de la celda:

- `datetime(Y,M,D)` con formato `d.m.yyyy` → código `D.M.Y` (ej: `datetime(2001,1,1)` → `"1.1.2001"`).
- `datetime(Y,M,D)` con formato `d.m` → código `D.M` (ej: `datetime(2026,1,2)` → `"2.1"`).
- entero/float `1.0..6.0` → código `"1".."6"` (raíces).
- texto ya no corrompido (ej. `"1.1.2004.01"`) → tal cual.

**Traducción adicional heredada de F3.3**: la madre `5.4` ("Otro Ingresos y Egresos") se movió a la raíz `6` al sembrar el plan de cuentas real — cualquier código decodificado que empiece con `5.4.` se traduce reemplazando ese prefijo por `6.` (ej. `5.4.4001` → `6.4001`).

**Validación cruzada hecha en esta sesión**: decodifiqué y traduje los **47 códigos distintos** de la hoja `Libro Diario` y los **30 códigos distintos** de `Libro Diario - sep25 a abr26` (77 en total, con 26 en común) contra el plan de cuentas ya sembrado en el sistema (`V17__contabilidad_seed_plan_de_cuentas.sql`) — **los 77 resuelven sin excepción** a una cuenta imputable existente. No hay ningún código huérfano. Esto confirma que la hoja "Plan de Cuentas" del Excel **ya fue migrada** (es la misma estructura, con las mismas 2 decisiones de diseño de F3.3 ya aplicadas: madre `3.1` agregada por simetría, y `6` como raíz nueva para "Otros Ingresos y Egresos").

---

## Orden de importación por dependencias (F10.2)

```
1. Jurisdicción, Moneda, Rubro, Categoría, CuentaContable, TipoCosto   (ya sembrados — solo validar, no migrar de nuevo)
2. Cliente, Proveedor, Comisionista, CuentaBancaria, Usuario           (maestros base)
3. Proyecto (requiere Cliente)
4. Etapa (requiere Proyecto) — ya tiene importador propio, ver Fase 2 más abajo
5. ComisionProyecto (requiere Proyecto + Comisionista)
6. PresupuestoProyecto + líneas de costo (requiere Proyecto)
7. Concepto recurrente (requiere CuentaContable)
8. CuentaBancaria.cuentaContable / Proyecto.responsable (ya cubiertos en 2-3, se mencionan por dependencia circular con Usuario)
9. Asiento + AsientoLinea — Libro Diario histórico ("sep25 a abr26") primero, después Libro Diario actual, en orden cronológico estricto por fecha
10. Inversion + MovimientoInversion (requiere CuentaBancaria; opcionalmente Compromiso/Vencimiento si hay vínculo)
11. Compromiso (requiere Proveedor/Proyecto opcional)
12. Vencimiento (requiere Moneda; opcionalmente CuentaContable/Proveedor/Proyecto/Concepto)
13. PendienteAdministrativo (requiere Proyecto/Cliente/Proveedor opcional)
14. LiquidacionIva / LiquidacionIibb — NO se migran como carga masiva (ver punto de decisión más abajo); si el equipo decide migrarlas, van al final porque referencian Asiento
```

---

## Mapeo hoja por hoja

### 1. `Clientes` → **Cliente + Proyecto + ProyectoCuota** — migra como dato definitivo

La hoja mezcla dos conceptos: cada fila es en realidad un **proyecto** de un cliente (el campo "Proyecto" suele venir como `"Cliente - (Fase: X)"`), no un cliente puro. Hay que separar cliente de proyecto explícitamente.

| Columna Excel | Campo destino | Transformación |
|---|---|---|
| `Proyecto` (texto compuesto) | `Cliente.nombre` (parte antes de `" - "` o `" ("`) + `Proyecto.nombre` (texto completo o con la fase) | Split manual por fila — no hay columna de cliente separada. Cruzar contra la hoja `Base de datos - Clientes` para `Cliente.razonSocial`/`cuit` cuando exista match por nombre. |
| `Responsable/s` | *(sin destino directo)* | `Proyecto.responsable` es FK a `Usuario`, no texto libre — este campo son nombres de personas del cliente, no un usuario del sistema. **No migra**, o pasa a `Proyecto.comentarios` si se quiere conservar. |
| `País` | `Proyecto.pais` (texto libre) y determina `Proyecto.tipoProyecto` | `"Argentina"` → `ARGENTINA`; cualquier otro país → `EXTERIOR` (afecta la cascada de F2.6). |
| `Detalle Tipo` | *(sin destino — no hay campo "tipo de empresa cliente" en Cliente)* | No migra como dato estructurado; puede ir a `comentarios`. |
| `Tipo de Persona` | *(sin destino directo en Cliente)* | No existe el campo; no migra. |
| `Condición frente a IVA` | *(sin destino — `Cliente` no tiene condición de IVA; sí `Proveedor.condicionIva`)* | **Gap real**: `Cliente` no tiene este campo hoy, `Proveedor` sí. No migra salvo que el equipo decida agregarlo a `Cliente` en un paso futuro (fuera de alcance de F10). |
| `Monto total proyecto sin IVA (USD)` | `Proyecto.montoTotal` + `Moneda` = USD | |
| `Pago 1..8` | `ProyectoCuota[]` (`numero`, `importe`) | `fechaEstimadaCobro` **no está en el Excel** — columna ausente. Ver punto de decisión. |
| `Comentarios` | `Proyecto.comentarios` | |
| — | `Proyecto.estado` | Inferir de las columnas: si `Monto total = 0` y sin comentario → `PROSPECTO`; si el comentario dice "FINALIZADO" → `FINALIZADO`; resto → `EN_CURSO`. Heurística, no dato explícito — **requiere revisión manual fila por fila** (28 proyectos activos + 11 potenciales = 39 filas, es abordable a mano). |

**Bloque "CLIENTES POTENCIALES" (filas 33-43)**: mismos campos, `Proyecto.estado = PROSPECTO`, `montoTotal = 0`.

**Inconsistencia real**: fila 26 (`LussoLift`) no tiene `Condición frente a IVA` cargada (celda vacía) a diferencia de las demás 27 filas — no es un error, es un dato faltante a completar por el equipo o dejar `null`.

---

### 2. `Base de datos - Clientes` → **Cliente** (complementa a la hoja anterior) — migra como dato definitivo

| Columna | Campo | Nota |
|---|---|---|
| `Nombre Clientes` | Clave de cruce contra `Clientes.Proyecto` (nombre del cliente, no del proyecto) | |
| `Razón Social` | `Cliente.nombre` (el sistema no distingue nombre comercial de razón social — un solo campo `nombre`) | Usar razón social si existe, si no el nombre comercial. |
| `CUIT` | `Cliente.cuit` | **13 de 15 filas tienen CUIT vacío** — obligatorio en el sistema (`@NotBlank @CuitValido`). Ver punto de decisión (placeholder vs. pedir el dato real). |

`Jurisdiccion` (FK obligatoria en `Cliente`) no está en ninguna de las dos hojas de clientes — todos los proyectos son de Argentina salvo excepciones puntuales (Puerto Rico, Colombia, EEUU); asumir jurisdicción por defecto del tenant y corregir a mano los casos exterior si corresponde (la jurisdicción fiscal argentina no aplica a operaciones de exportación, pero el campo es obligatorio en el modelo actual).

---

### 3. `Proveedores de servicios` → **Proveedor + relación proyecto/costo** — parcialmente migra

| Columna | Campo | Nota |
|---|---|---|
| `Proveedor` (ej. `"BASILOTTA MATIAS"`) | `Proveedor.nombre` | 8 proveedores distintos, sin CUIT/jurisdicción en la hoja (ambos obligatorios en `Proveedor`) — mismo gap que Clientes. |
| `Proyecto` + `Servicio a brindar` | *(sin entidad destino exacta)* | No hay una entidad "costo presupuestado por proveedor y proyecto" — lo más cercano es `PresupuestoLineaCosto` (texto libre `nombre` + `importeUsd`), pero esa es en USD *agregado*, no por proveedor con pagos parciales. |
| `Monto total del servicio sin IVA` + `Pago 1..8` | `PresupuestoLineaCosto.importeUsd` (una línea por proveedor+servicio, sin desglose de pagos — el sistema no modela cuotas de costo, solo de cobro) | La mayoría de las filas tienen esta columna vacía (solo 2 de 18 tienen monto). |

**Clasificación**: **define estructura** (los proveedores sí migran como maestro), los montos/pagos **no migran 1:1** porque el modelo de F2.6 no tiene un concepto de "cuotas de costo por proveedor" — solo total de línea de costo. Requiere decisión del equipo: ¿se resigna el detalle por-pago del costo, o se registra como `Compromiso` individual (`tipo=PAGO_A_PROVEEDOR`, `proveedorId`, `proyectoId`) por cada pago con importe > 0? Esto último sí es fiel al dato pero serían ~15 Compromisos con fecha desconocida (la hoja no tiene fechas de pago).

---

### 4. `Comisiones por ventas` → **ComisionProyecto** — migra parcialmente como dato

| Columna | Campo | Nota |
|---|---|---|
| `Proyecto` | Cruzar contra `Proyecto.nombre` (mismo texto que en `Clientes`) | |
| `Comisionista` | `Comisionista.nombre` → FK | Solo 2 comisionistas nombrados: "Cristian Pittaluga" (10%) y una comisión de "Javier Montanari" al 20% mencionada en el comentario de la hoja (R3) pero **sin fila propia** — no hay una fila con `Comisionista = Javier Montanari` en los datos. |
| `% Comisión` | `ComisionProyecto.porcentajeComision` | Presente solo en 2 de 14 filas (el resto de comisiones están en USD directo, sin %). |
| `Monto total de la Comisión` | `ComisionProyecto.importeEstimado` o `importeFinal` | Columna `Comentarios` a veces dice `"En dols"` — moneda USD; el resto son montos en ARS (`baseCalculo` no se puede inferir con certeza). |

**Inconsistencia real**: la regla de negocio declarada en R3 ("los comisionistas se llevan 10%, Javier Montanari el 20%") **no se refleja en ninguna fila de datos** — no hay comisiones cargadas para Javier Montanari en absoluto, pese a la regla explícita. Falta `fechaEstimadaPago` en todas las filas (columna inexistente). **No migra automáticamente**: hay que decidir con el equipo si se cargan como `ComisionProyecto` sin fecha (el campo es opcional) o se completa a mano.

---

### 5. `Presupuesto de Pagos` → **Compromiso** (sección 1) + **Concepto recurrente** (secciones 2-6) — mixto

**Sección "1. Plan de Pagos Impuesto a las Ganancias"** (filas 7-21): cuotas concretas con fecha de vencimiento exacta → **migra como dato definitivo**, una fila = un `Compromiso` (`tipo=CUOTA_PLAN_DE_PAGOS`, `fechaPrevista` = "Fecha de Vencimiento", `importe` = "Totales"). 12 cuotas con fecha real.

**Secciones 2-6** (Sueldos, Honorarios contador, Anticipo IG, Comisión bancaria, IVA e IIBB — filas 29-50): son proyecciones mensuales recurrentes de junio a diciembre 2026, sin fecha de vencimiento puntual (solo "mes"). Esto **no migra fila por fila como `Compromiso`** — el sistema ya tiene el mecanismo correcto para esto: **`Concepto` recurrente** (`periodicidad=MENSUAL`, con su `CuentaContable` y monto), que F8.1 usa para autogenerar `Vencimiento` mes a mes. **Clasificación: solo define estructura** — la acción correcta es dar de alta 5 `Concepto`s recurrentes (uno por rubro: Sueldos, Honorarios, Anticipo IG, Comisión bancaria, IVA/IIBB) con el monto vigente, no 35 filas de dato histórico proyectado.

---

### 6. `Inversiones en Fondos Fima` → **Inversion + MovimientoInversion** — migra como dato definitivo

Coincidencia estructural casi 1:1 con el modelo del sistema (F8.4):

| Columna | Campo | Transformación |
|---|---|---|
| `Fondo` (ej. "Fima Premium") | `Inversion.instrumento` | Una sola `Inversion` para "Fima Premium" (todas las filas la comparten). |
| `Detalle` (ej. "IVA 04.26") | `Inversion.objetivoDelDinero` | Sugiere vínculo con una obligación de IVA — si existe el `Vencimiento`/`Compromiso` de ese período, completar `vinculoTipo=VENCIMIENTO`/`COMPROMISO` + `vinculoRefId`. |
| `Operación` (`Agregar`/`Retirar`) | `MovimientoInversion.tipo` | `Agregar` → `SUSCRIPCION`, `Retirar` → `RESCATE`. **Fila "Valuacion del Fondo Fima" (operación vacía) no es un movimiento real** — es una revaluación de cuotaparte sin cambio de tenencia, no migra como `MovimientoInversion` (el sistema no modela "revaluación sin movimiento": la valuación se recalcula sola a partir del último `valorCuotaparte` cargado). |
| `Fecha de liquidación` | `MovimientoInversion.fecha` | |
| `Cuotapartes` | `MovimientoInversion.cuotapartes` | |
| `Valor cuotaparte` | `MovimientoInversion.valorCuotaparte` | |
| `Monto` | `MovimientoInversion.montoAplicado` | |
| `cuentaOrigen` | *(no está en la hoja)* | Se asume la cuenta bancaria principal (Banco Galicia CC) — confirmar con el equipo. |

**Inconsistencia real**: fila `R8` ("IVA 05.26 - FALTABA") tiene `Cuotapartes = 20919.23` con signo negativo en una columna intermedia (`-0.003024500795`, probablemente una diferencia de redondeo de la fórmula de Excel) — no afecta el mapeo pero conviene que el equipo confirme el monto exacto antes de cargar.

---

### 7. `Libro Diario` + `Libro Diario - sep25 a abr26` → **Asiento + AsientoLinea** — migra como dato definitivo (el núcleo de F10.2)

| Columna | Campo | Transformación |
|---|---|---|
| `N° Asiento` | Agrupador — todas las líneas con el mismo número forman un `Asiento` | Renumerar: el número original de Excel **no se reusa**, `Asiento.numero` lo asigna el sistema al confirmar (secuencia propia). |
| `Fecha` | `Asiento.fecha` | Ya viene como fecha real de Excel, sin corrupción. |
| `Código` | `AsientoLinea.cuentaContable` | Decodificar con el algoritmo de la sección de arriba + traducción `5.4.X → 6.X`. **Ya validado: 100% de los códigos usados resuelven.** |
| `Conceptos` | *(no se usa — es el nombre de la cuenta, redundante con `Código` ya decodificado; sirve solo para verificación cruzada)* | |
| `Debe` / `Haber` | `AsientoLinea.debe` / `.haber` | |
| `Leyenda` | `AsientoLinea.leyenda` | |
| `Destino de los Fondos (NO TOCAR)` | `AsientoLinea.cuentaBancaria` (cuando aplica) | Texto libre en la hoja (ej. "Otros Créditos y gastos bancarios", "Percepciones IVA Locales") — **no es una cuenta bancaria**, es una referencia a un concepto de conciliación (F5.3). No mapea a `cuentaBancaria`; **no migra**, se pierde esa referencia o se preserva en `leyenda` si se considera valiosa. |
| `Moneda` | *(no está en la hoja — todo está en ARS)* | Asumir `Moneda=ARS`, `tipoCambio=1` para todas las líneas. |
| `Origen` | `Asiento.origen = MANUAL` en todos los casos (no hay forma de saber si un asiento salió de una factura importada) — ver punto de decisión sobre si conviene marcarlos `IMPORTACION` en cambio (el enum ya tiene ese valor, pensado exactamente para esto). |

**Inconsistencia real, ya detectada y confirmada por análisis directo (no por la hoja)**: hay un **agujero de ~3 meses sin ningún asiento** — `Libro Diario - sep25 a abr26` termina el 30/09/2025 (pese a que su nombre promete llegar hasta abril 2026) y `Libro Diario` recién arranca el 13/01/2026. **Octubre, noviembre y diciembre de 2025 no tienen ningún asiento en ninguna de las dos hojas.** Esto es crítico: cualquier saldo inicial calculado a partir de este libro diario va a estar mal si esos 3 meses tuvieron movimientos reales no registrados. **Requiere confirmación explícita del equipo**: ¿esos meses realmente no tuvieron actividad contable, o falta cargar/encontrar esos asientos en otro lado?

**Filas basura**: ambas hojas tienen cientos de filas finales con `Código = '#N/A'` y el resto vacío (residuo de fórmulas de Excel extendidas más allá de los datos reales) — se descartan en el parser (ya lo hace el patrón `EtapaImportExcelParser` con `DataFormatter`/validación de fila vacía).

---

### 8. `Plan de Cuentas` → **CuentaContable** — ya migrado, no se vuelve a migrar

Confirmado por decodificación + validación cruzada (ver sección superior): esta hoja **es** el plan de cuentas ya sembrado en `V17__contabilidad_seed_plan_de_cuentas.sql`, con las 2 decisiones de F3.3 ya aplicadas. **Clasificación: no migra** — solo sirve como referencia de auditoría para confirmar que el seed no perdió ninguna cuenta (confirmado: no perdió ninguna).

---

### 9. `MAYORES` → no migra

Una sola columna de números sin ningún encabezado ni referencia a cuenta — residuo de una vista anterior, inutilizable como dato. El sistema ya genera Mayores en vivo desde `Asiento`/`AsientoLinea` (F3.6). **No migra.**

---

### 10. `IVA a pagar` / `IIBB a pagar` → **no migra como carga masiva** (punto de decisión)

Ambas hojas son el cálculo mensual de IVA/IIBB — estructuralmente iguales a lo que `CalculoIvaService`/`CalculoIibbService` ya recalculan **en vivo** a partir de los asientos importados. `LiquidacionIva`/`LiquidacionIibb` **no tienen `CrearRequest`** (se generan por período, no por carga manual), así que migrarlas como dato requeriría construir un endpoint de carga que hoy no existe — fuera del alcance natural de F10.2.

**Recomendación**: no migrar estas dos hojas como dato. Una vez importado el Libro Diario, generar las liquidaciones de los períodos históricos con el motor real del sistema y usar estas hojas **solo para validar** que el cálculo coincide (mismo criterio que la calibración hecha en F6.1/F6.2 contra la hoja real del contador). Requiere confirmación del equipo.

---

### 11. `Estado de Resultados Mensual` (×2), `Estado de Situacion Patrimonial` → no migran (punto de decisión + inconsistencia real)

Igual que el punto anterior: son vistas calculadas que el sistema ya recalcula en vivo (F7.2/F7.3) desde los asientos. **No deberían migrar como dato.**

**Inconsistencia real seria**: existen **dos hojas con el mismo nombre** ("Estado de Resultados Mensual" y "Estado de Resultados Mensual  -", esta última con espacios/guion final) que **traen números distintos para los mismos meses** — ejemplo, "Resultado Bruta" de junio: **9.008.880,98** en una hoja vs **15.179.080,98** en la otra (diferencia de más de 6 millones). Esto indica que una de las dos quedó desactualizada o es un borrador de prueba. **El equipo debe confirmar cuál (si alguna) es la vigente** antes de usarla como referencia de validación — no importa para la migración de datos (ninguna de las dos migra), pero si se van a usar para validar el motor de ER una vez importado el Libro Diario, hay que saber contra cuál comparar.

---

### 12. `Flujo de Caja Proyectado` / `Flujo de Caja Detallado Mensual` → no migran

Mismo criterio: F8.3 ya calcula flujo de caja real y proyectado en vivo desde `Compromiso`/`Vencimiento`/`Cobro`/`Pago`/`Inversion`/`MovimientoBancario`. Estas hojas son la versión manual que el sistema reemplaza. **No migran como dato** — sirven de referencia para validar el flujo proyectado una vez cargados Compromisos/Vencimientos/Inversiones.

---

### 13. `CALENDARIO DE VENCIMIENTOS` → no migra (vacía)

Solo tiene encabezados, sin ninguna fila de dato real. Nada que migrar — el sistema ya tiene el motor de vencimientos (F8.1) funcionando.

---

### 14. `PENDIENTES` + `PENDIENTES AHORA` → **PendienteAdministrativo** — migra como dato

Ambas hojas son listas de notas sueltas en texto libre (18 ítems entre las dos). Mapean directo: cada fila → `PendienteAdministrativo.titulo` = el texto de la nota, `categoria` = null o "General" (texto libre, sin estructura clara en el origen), `fechaEstimadaResolucion` = null (ninguna nota tiene fecha), `prioridad = MEDIA` por defecto. **Migra como dato definitivo**, sin transformación compleja — es la carga más directa de todo el Excel.

---

### 15. `ANALISIS DE SUELDOS` → no migra

Es una planilla de reconciliación personal entre "Recibos" (lo que se le debería pagar a Franco Montanari por sueldo) y "Banco" (lo efectivamente acreditado), con una columna de control de diferencia — **no corresponde a ninguna entidad del sistema** (no hay módulo de nómina/RRHH). Los sueldos ya están representados como líneas de `Asiento` (cuenta "Sueldos - Franco Montanari") en el Libro Diario. **No migra**; si se quiere conservar el historial de reconciliación se puede adjuntar el Excel original como referencia, no como dato estructurado.

---

## Resumen de clasificación

| Hoja | Clasificación |
|---|---|
| Clientes | Migra como dato (con transformación manual de split cliente/proyecto) |
| Base de datos - Clientes | Migra como dato (complementa Clientes) |
| Proveedores de servicios | Migra parcial — proveedores sí, costos por pago requieren decisión |
| Comisiones por ventas | Migra parcial — requiere completar fechas/decisión sobre comisión faltante de Javier Montanari |
| Presupuesto de Pagos §1 | Migra como dato (Compromiso) |
| Presupuesto de Pagos §2-6 | Define estructura (Concepto recurrente, no carga histórica) |
| Inversiones en Fondos Fima | Migra como dato |
| Libro Diario (ambas hojas) | Migra como dato — núcleo de la migración |
| Plan de Cuentas | Ya migrado (validado, no se repite) |
| MAYORES | No migra |
| IVA a pagar / IIBB a pagar | No migra (recomendado) — usar para validar |
| Estado de Resultados Mensual (×2) | No migra — inconsistencia real entre las dos versiones, resolver antes de usar como referencia |
| Estado de Situacion Patrimonial | No migra — usar para validar |
| Flujo de Caja Proyectado / Detallado | No migra — usar para validar |
| CALENDARIO DE VENCIMIENTOS | No migra (vacía) |
| PENDIENTES / PENDIENTES AHORA | Migra como dato (PendienteAdministrativo) |
| ANALISIS DE SUELDOS | No migra |
| Proyectos | **No migra** (credenciales + datos ya cubiertos por "Clientes") |

---

## Puntos que el equipo debe decidir antes de F10.2 — **resueltos**

1. **Agujero de 3 meses sin asientos (oct-dic 2025)**: hubo actividad real en ese período, pero no quedó pasada al Excel. **No se migra este tramo desde el Excel** — se va a cargar a mano una vez que el sistema esté operativo. F10.2 no necesita reconciliar ni buscar estos asientos; el Libro Diario migrado va a tener ese hueco por diseño.
2. **Dos hojas "Estado de Resultados Mensual" con números distintos**: la vigente es **`Estado de Resultados Mensual`** (sin el espacio/guion final). La otra (`Estado de Resultados Mensual  -`) no se usa ni para migrar ni para validar.
3. **IVA a pagar / IIBB a pagar / Estados / Flujo de Caja**: confirmado — **no migran como dato**. El sistema los arma en base a los asientos importados (mismo criterio ya usado en F6.1/F6.2/F7.2/F7.3/F8.3).
4. **CUIT faltante en 13 de 15 clientes**: **se omite** — los clientes (y sus CUIT reales) se van a importar/completar después, no es un bloqueante de F10.2.
5. **Proveedores sin CUIT/jurisdicción**: mismo criterio — **se omite**, se completa en una importación posterior.
6. **Costos por proveedor y proyecto** (pagos parciales sin fecha): se resuelve alineado al mismo patrón que `ProyectoCuota` — **se crea la cantidad de pagos/líneas de costo al crear el proyecto, sin fecha de pago** (coincide con que `PresupuestoLineaCosto` tampoco tiene campo de fecha en el modelo actual).
7. **Comisión de Javier Montanari (20%)**: **se omite** — no se carga sin una fila de dato concreta que la respalde.
8. **Origen de los Asiento migrados**: **`MANUAL`** (no `IMPORTACION`).
9. **Credenciales bancarias en la hoja "Proyectos"**: **no se consideran en la importación** (la hoja completa queda fuera del alcance de F10.2, como ya estaba clasificado). Recordatorio aparte: rotar esas credenciales sigue pendiente y es responsabilidad del equipo, fuera del alcance de este paso.

---

## Nota sobre el importador (para F10.2)

No hace falta construir el parser de Excel desde cero: el proyecto ya tiene un patrón reusable de F2.5 (`EtapaImportParser`/`EtapaImportExcelParser`, Apache POI ya declarado en `pom.xml`), con el flujo correcto — **previsualizar (parsear + validar sin persistir) → confirmar (persistir solo filas sin error)** — y normalización ya resuelta de fechas, importes en formato es-AR/en-US, y resolución de referencias por nombre. F10.2 puede calcar esa arquitectura por cada entidad de este mapeo, reemplazando el mapeo de columnas fijo de Etapa por el de cada hoja.
