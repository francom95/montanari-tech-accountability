# 5. Liquidación de IVA e IIBB

## ¿Para qué sirve?

Estas dos pantallas arman automáticamente la liquidación mensual de IVA y de Ingresos Brutos (Convenio Multilateral), calculando todo a partir de las facturas y asientos ya confirmados del mes. Podés ajustar cualquier concepto a mano (dejando un motivo), y al confirmar, el sistema genera el asiento contable de la liquidación.

- IVA: menú **Impuestos → IVA**.
- IIBB: menú **Impuestos → IIBB**.

## Liquidación de IVA

1. Elegí **Año** y **Mes**, y hacé clic en **"Liquidar este período"**. Si ya existe una liquidación de ese mes (en borrador o confirmada), el botón se desactiva.
2. Antes de crearla, podés ver una **previsualización** de solo lectura con los mismos números.
3. La liquidación muestra los componentes en dos grupos:
   - **Etapa técnica** (determina el impuesto del período): Débito fiscal, Crédito fiscal, ajustes por notas de crédito, saldo técnico del período anterior.
   - **Ingresos directos**: percepciones y retenciones de IVA sufridas, saldo de libre disponibilidad del período anterior.
4. Cada fila tiene una columna **Calculado** (lo que armó el sistema solo), una columna **Ajuste** (donde vos podés corregir, siempre con un **Motivo**), y el resultado **Final**. Con "Guardar" por fila se aplica el ajuste.
5. En borrador, también podés agregar un concepto manual con **"Agregar"** (por ejemplo, algo que el sistema no detecta solo), indicando cuenta contable.
6. El resultado se muestra en tres líneas separadas — **no se suman entre sí**, son cosas distintas:
   - **Saldo a pagar**
   - **Saldo técnico a favor** (solo se puede usar contra IVA de los próximos meses)
   - **Saldo de libre disponibilidad** (se puede usar contra otros impuestos o pedir la devolución)
7. Con **"Confirmar y generar asiento"** la liquidación queda cerrada y se contabiliza sola. Ya no se puede editar.
8. Si necesitás deshacerla, usá **"Des-confirmar (anula el asiento)"** con un motivo — no vuelve a borrador, queda anulada; para reliquidar ese mes hay que crear una nueva.

📷 *(Captura: pantalla de liquidación de IVA con los componentes editables y los tres resultados finales)*

## Liquidación de IIBB (Convenio Multilateral)

Funciona igual (Año/Mes → "Liquidar este período"), pero organizado **por jurisdicción**: cada jurisdicción tiene su propia tarjeta con:
- **Coeficiente** (qué porcentaje de la actividad le corresponde) y **Alícuota %** — ambos editables mientras está en borrador, con botón "Aplicar".
- Base imponible e Impuesto determinado (calculados solos).
- Deducciones editables: Percepciones, Retenciones, SIRCREB, Pagos a cuenta, Saldo a favor anterior, Otro — cada una con su Ajuste + Motivo.
- Al pie de cada jurisdicción: Saldo a pagar o Saldo a favor (que se arrastra al mes siguiente).

**Sobre el coeficiente:** por defecto, el sistema usa qué porcentaje de las ventas del mes fueron a cada jurisdicción. Si ya existe una liquidación confirmada del mes anterior, hereda el coeficiente real de Convenio Multilateral de esa liquidación (nunca mezcla los dos criterios entre jurisdicciones distintas del mismo mes).

Al confirmar, se genera el asiento igual que en IVA. También se puede des-confirmar con motivo.

📷 *(Captura: pantalla de liquidación de IIBB mostrando dos o más jurisdicciones con su coeficiente y base imponible)*

## Avisos que pueden aparecer (no bloquean, pero conviene revisarlos)

- *"La suma de los coeficientes de Convenio Multilateral no es 1"* — revisá los coeficientes de todas las jurisdicciones antes de confirmar.
- *"No hay una liquidación confirmada del mes anterior, así que los saldos arrastrados entran en cero"* — si venías con saldo a favor real, hay que cargarlo a mano como ajuste.
- *"Hay ventas sin jurisdicción de destino"* o *"hay ventas dirigidas a jurisdicciones inactivas"* — revisá que las facturas tengan bien cargada la jurisdicción, o reactivá la jurisdicción en el maestro.
- *"Se excluyeron ventas de exportación de la base de IIBB"* — es correcto, las facturas de exportación no pagan IIBB.

## Errores más comunes

- **"Ya existe una liquidación de este mes en borrador o confirmada"** — no se puede crear dos liquidaciones del mismo mes; si hay que rehacerla, anulá la anterior primero.
- **"Un ajuste manual necesita un motivo"** — todo ajuste a un valor calculado por el sistema exige explicar por qué se corrigió (queda en la auditoría).
- **"Solo se puede [editar / ajustar / agregar conceptos a] una liquidación en borrador"** — una vez confirmada, la liquidación queda de solo lectura; si hace falta corregir algo, primero hay que des-confirmarla.
- **"El período sigue abierto"** (aviso, no error) — es normal si estás liquidando el impuesto antes de cerrar el mes contable; no impide confirmar la liquidación.

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
