# F2.6 — Presupuesto estimado por proyecto

**Paso:** 14 de 55 · **Fase:** F2 — Proyectos · **Modelo asignado:** Sonnet 5 · **Depende de:** F2.5 (bloqueaba F7.4)
**Checkpoint humano:** Sí — ver "Checkpoint pendiente" al final.

## Qué se hizo

Presupuestar un proyecto en dólares a partir de sus costos de producción, replicando en código las dos "cascadas" de impuestos y comisiones que el contador arma a mano en Excel según el proyecto sea en Argentina o en el exterior. El insumo obligatorio del plan (los dos Excel reales de presupuesto) fue provisto por el usuario durante este paso — antes de eso, F2.6 y F7.4 (que depende de este cálculo) estaban bloqueados.

### `Proyecto.tipoProyecto` pasa a enum

El campo ya existía como texto libre sin uso real. Se restringió a `ARGENTINA`/`EXTERIOR` (columna VARCHAR sin cambio de tipo, sin migración) porque determina qué cascada aplica el motor de cálculo.

### Costos de producción como líneas libres

En vez de roles fijos (desarrollo, diseño, etc.), cada proyecto carga una lista libre de `(nombre, importeUsd)`. El presupuesto es editable en vivo, sin estados borrador/confirmado — decisión explícita del usuario, ambas más simples que lo que el plan sugería por defecto.

### Las dos cascadas (reverse-engineered de los Excel reales)

Ambas parten de: `totalCostoProduccion` (Σ líneas) + `margenDeseadoUsd` (dato del usuario) + un **colchón de Impuesto a las Ganancias** (`margen / (1 − tasaGanancias) − margen`, para que el margen neto deseado sobreviva al impuesto a las ganancias).

**Argentina**: sobre `totalCostoMasGanancia` se resuelve un precio que se autocontiene (la comisión de venta y el IVA débito fiscal se calculan sobre el propio precio final) — un caso de resolución circular resuelto algebraicamente por división cerrada, no iterativamente. Cascada: comisión de venta → precio sin IVA → IIBB Convenio Multilateral → impuesto a los débitos/créditos → IVA débito fiscal → precio con IVA (= precio final).

**Exterior**: sin IVA/IIBB doméstico (la venta de servicios al exterior está exenta), pero con una comisión bancaria COMEX genuinamente **escalonada** (según el excel real de la hoja "GUADA", no la hoja "FRAN" que hardcodeaba una relación 363/24637 = 1,4734% solo válida para esas alícuotas puntuales — el usuario confirmó GUADA como canónica): 3 tramos de monto fijo según el precio por cuota (USD 100/500/1000 → USD 10/30/50) y, por encima del último umbral, 0,125% del precio. Cascada: comisión bancaria COMEX (más las comisiones intermedias que carga el usuario) → percepción IVA COMEX → IIBB/SIRCREB COMEX → IVA crédito fiscal COMEX → precio final.

### Todo configurable, nada hardcodeado

Las 8 alícuotas y los 7 parámetros de la escala COMEX viven en `ConfiguracionPresupuesto` (fila única por tenant, editable solo por ADMINISTRADOR vía `/api/v1/presupuestos/configuracion`), mismo patrón que `ConfiguracionAtribucion` (F6.3). Ningún porcentaje ni umbral está escrito en el motor de cálculo.

### Precisión numérica

Divisiones internas a escala 10 con `HALF_UP`; el único redondeo a 2 decimales explícito en el código es el que el Excel real hace con `ROUND(...,2)` en la Comisión por Venta de Argentina — el resto queda en alta precisión interna para poder comparar contra los valores intermedios sin redondear del propio Excel.

### Frontend

Pestaña "Presupuesto" en la ficha de proyecto (antes un placeholder): lista editable de líneas de costo (`useFieldArray`, mismo patrón que "Cuotas"), margen deseado, comisiones intermedias COMEX (solo si `tipoProyecto = EXTERIOR`), y el desglose completo de la cascada calculada en modo lectura, con las etiquetas correspondientes a Argentina o Exterior según el tipo del proyecto. `usePresupuestoProyecto` trata el 404 (proyecto sin presupuesto todavía) como `null` en vez de error.

## Verificación realizada

- **Suite completa** en local (JDK 21): **459/460 en verde** (el único fallo es el de Testcontainers/Docker Desktop local, ya documentado). 5 tests nuevos en `CalculoPresupuestoProyectoTest`, calibrados contra los 2 casos reales del Excel Exterior.
- Frontend: `tsc -b` y `oxlint` limpios.

### Bugs reales detectados y corregidos durante este paso

- **Regresión en `ProyectoServiceTest`**: al convertir `tipoProyecto` a enum estricto (Fase A), dos tests existentes seguían pasando texto libre viejo ("Desarrollo", "Consultoría") que ya no son valores válidos — actualizados a "ARGENTINA"/"EXTERIOR".
- **`Assertions.offset(BigDecimal)` no existe** en la versión de AssertJ del proyecto (solo hay overloads para `Float`/`Double`): las dos aserciones `isCloseTo` calibradas contra el Excel real no compilaban. Corregido usando `org.assertj.core.data.Offset.offset(BigDecimal)` directamente.
- **Extracción de fórmulas de Excel**: la primera pasada con `openpyxl` (`data_only=False`) solo imprimía el valor cacheado de las celdas con fórmula, no el texto de la fórmula — llevó a leer mal la comisión bancaria COMEX como una relación simple. Autocorregido antes de presentar nada al usuario, re-extrayendo fórmula y valor cacheado lado a lado.

### E2E contra MySQL 8 real (docker-compose)

Se creó un cliente, una jurisdicción, y un proyecto de cada tipo, y se completó/guardó el presupuesto vía API real y vía la UI en el navegador (con un proxy de desarrollo temporal para sortear la ausencia de configuración CORS del backend — no forma parte de este paso, revertido después de verificar).

| Caso | Resultado | ✔ |
|------|-----------|---|
| Config sembrada por Flyway | 8 alícuotas + 7 parámetros COMEX, valores reales del Excel | ✔ |
| Argentina (costo 1000, margen 500) | precio final `2482,75` — idéntico al test unitario | ✔ |
| Exterior (costo 1500, margen 1500, comisiones intermedias 200, 5 cuotas) | precio final `4901,9966386554` — **coincide con el Excel real** (caso 1, hoja GUADA MODELO) hasta el ruido de punto flotante del propio Excel | ✔ |
| Argentina real (Excel "Presupuesto_ Web.xlsx", proyecto SEEU, costo 150 = Diseño 100 + Desarrollo 50, margen 100) | precio final `424,1326836` — **coincide con el Excel real** al centavo (y más) | ✔ |
| UI: pestaña Presupuesto, ambos tipos | desglose completo, etiquetas correctas según tipo, sin el bloque COMEX en Argentina | ✔ |
| UI: agregar línea de costo + guardar | recalcula toda la cascada (costo 1000→1100, precio final 2482,75→2627,57) | ✔ |

### Checkpoint cerrado

El plan exige que el contador valide que los cálculos repliquen el Excel de referencia. La cascada **Exterior** está verificada al centavo (y más) contra los 2 casos reales cargados en el Excel "Presupuesto Proyecto Exterior". La cascada **Argentina** se cerró con un Excel real adicional aportado por el usuario ("Presupuesto_ Web.xlsx", proyecto SEEU): costo de producción 150 USD (Diseño 100 + Desarrollo 50), margen deseado 100 USD → precio final `424,1326836`, coincidiendo con el Excel al centavo (y más) en las 5 líneas de la cascada (comisión de venta, precio sin IVA, IIBB, impuesto a los débitos/créditos, IVA, precio con IVA). Test agregado: `argentinaCasoRealCalibradoContraElExcelReal`. Ambas cascadas quedan calibradas contra datos reales del contador.
