# 12. Preguntas frecuentes: los 10 errores de carga más comunes

Estos son los mensajes con los que más probablemente te vas a topar al usar el sistema día a día, y cómo solucionarlos. Todos son mensajes de **prevención**, no de "algo se rompió" — el sistema está frenando antes de guardar algo inconsistente.

## 1. "La factura / el cobro / el pago no tiene ningún importe a contabilizar"

**Cuándo pasa:** al confirmar una factura, cobro o pago que quedó sin líneas cargadas, o con todas las líneas en cero.

**Cómo corregirlo:** volvé al borrador, revisá que tenga al menos una línea con un importe mayor a cero, y confirmá de nuevo.

## 2. "No hay una cuenta configurada para el concepto..."

**Cuándo pasa:** al confirmar, si el sistema necesita una cuenta contable para algún concepto (un tipo de costo, un tipo de ingreso, un tributo) que todavía no tiene una cuenta asignada en la configuración.

**Cómo corregirlo:** pedile al administrador del sistema que complete el mapeo de cuentas para ese concepto (menú de administración de cuentas contables), y volvé a confirmar.

## 3. "Solo se pueden editar o eliminar [facturas/cobros/pagos/liquidaciones] en borrador"

**Cuándo pasa:** cuando intentás modificar algo que ya fue confirmado (o anulado).

**Cómo corregirlo:** una vez confirmado, ya no se edita — se **anula** (con un motivo) y se carga de nuevo si hace falta corregir algo.

## 4. "La suma de las imputaciones no puede superar el total cobrado/pagado"

**Cuándo pasa:** al cargar un cobro o pago, si la suma de lo que imputaste contra facturas es mayor al total que efectivamente cobraste o pagaste.

**Cómo corregirlo:** revisá los montos de cada línea de imputación; entre todas no pueden superar el total cargado arriba.

## 5. "La factura... no está confirmada" (al imputar un cobro o pago)

**Cuándo pasa:** solo se puede imputar contra facturas ya confirmadas — no contra un borrador.

**Cómo corregirlo:** confirmá primero la factura de venta o compra, y después volvé a cargar el cobro o pago.

## 6. "Falta el tipo de cambio para importar filas en USD"

**Cuándo pasa:** al importar un resumen bancario que trae movimientos en dólares.

**Cómo corregirlo:** en la pantalla de previsualización de la importación, completá el campo de tipo de cambio antes de hacer clic en "Confirmar importación".

## 7. "No se reconocen las columnas esperadas" / "No se pudo leer el resumen..."

**Cuándo pasa:** al subir un archivo de resumen bancario que no tiene el formato que el sistema espera para ese banco (por ejemplo, se editó el Excel, o se subió el archivo equivocado — un PDF donde correspondía un Excel, o el resumen de otro banco).

**Cómo corregirlo:** volvé a descargar el resumen original directamente del banco/Mercado Pago, sin modificarlo, y subí ese archivo tal cual.

## 8. "Este movimiento no tiene fecha cargada" (en la bandeja de movimientos bancarios)

**Cuándo pasa:** algunos resúmenes no traen la fecha de cada movimiento en el archivo — el movimiento queda pendiente sin fecha.

**Cómo corregirlo:** usá el botón "Editar" en ese movimiento para completar la fecha; recién ahí se puede Confirmar o Imputar.

## 9. "El período está cerrado"

**Cuándo pasa:** al intentar cargar, editar o anular algo con fecha de un mes que el contador ya cerró.

**Cómo corregirlo:** si realmente hace falta cargarlo en ese mes, pedile a un administrador que lo autorice (aparece un botón para confirmar igual, con un motivo obligatorio). Si no es urgente, cargalo con la fecha del mes actual en su lugar.

## 10. "Un ajuste manual necesita un motivo" (en liquidación de IVA/IIBB)

**Cuándo pasa:** al corregir a mano un valor que el sistema calculó automáticamente en la liquidación de impuestos.

**Cómo corregirlo:** completá el campo de motivo antes de guardar — queda registrado en la auditoría para que después se entienda por qué se ajustó.

---

Si te encontrás con un mensaje que no está en esta lista, cada capítulo del manual tiene su propia sección de "Errores más comunes" con más detalle específico de ese módulo.
