# 8. Presupuestos de proyectos y rentabilidad

## ¿Para qué sirve?

Dentro de cada proyecto, hay una pestaña para armar el presupuesto estimado (cuánto le vamos a cobrar al cliente para cubrir costos + margen + impuestos) y otra para ver, una vez que el proyecto avanza, cómo viene la rentabilidad real comparada contra lo presupuestado.

Menú: entrá al proyecto desde **Proyectos**, y usá las pestañas "Presupuesto" y "Rentabilidad".

## Armar el presupuesto de un proyecto

1. En la pestaña **"Presupuesto"**, cargá las **"Costos de producción"**: una línea por cada concepto de costo, con su Concepto e Importe en USD. Usá **"+ Agregar línea de costo"** para sumar más, y "Quitar" para sacar una.
2. Cargá el **"Margen deseado (USD)"** — cuánto margen de ganancia querés sobre el costo.
3. Si el proyecto es del tipo **Exterior** (con clientes o pagos que involucran comercio exterior), aparece un campo extra: **"Comisiones bancarias intermedias COMEX (USD)"**.
4. Hacé clic en **"Guardar presupuesto"**.

El sistema calcula automáticamente, en una tarjeta aparte, la cascada completa de precios (distinta según el proyecto sea "Argentina" o "Exterior"): comisión de venta, colchón de Impuesto a las Ganancias, IIBB, IVA, comisiones bancarias, hasta llegar al **"Precio final al cliente (USD)"**. Este cálculo se recalcula en vivo — no hay que guardarlo aparte, ya que se arma con los porcentajes configurados del sistema.

📷 *(Captura: pestaña "Presupuesto" de un proyecto, con las líneas de costo y la cascada de cálculo)*

## Leer el reporte de rentabilidad

En la pestaña **"Rentabilidad"** vas a encontrar:

- **Ingresos**: total facturado, total cobrado y pendiente de cobro (todo en pesos), más cuántas facturas están confirmadas y cuántas ya saldadas.
- **Egresos**: total facturado de compra, total pagado y pendiente de pago, con el detalle por cada proveedor.
- **Comisiones e impuestos atribuidos**: cuánto le corresponde a cada comisionista, y el total de impuestos atribuidos a este proyecto.
- **Presupuesto vs. real** (solo si el proyecto tiene presupuesto cargado): compara el precio presupuestado contra lo realmente facturado, mostrando cuántos pagos ya se pudieron emparejar contra una factura real.
- **Margen real**: el número final, la ganancia real del proyecto a la fecha.

Si el sistema detecta algo raro (por ejemplo, datos incompletos para hacer una comparación exacta), puede mostrar avisos arriba del reporte — conviene leerlos antes de sacar conclusiones del número final.

Podés exportar este reporte con los botones **"Exportar Excel"** o **"Exportar PDF"**.

📷 *(Captura: pestaña "Rentabilidad" con las secciones de ingresos, egresos y margen real)*

## Errores más comunes

- **"Falta la configuración de presupuesto del sistema"** — el administrador todavía no cargó los porcentajes generales (comisión de venta, IIBB, IVA, etc.) que usa la cascada de cálculo. Hay que pedirle que los complete.
- **Campos "Obligatorio" en rojo al guardar** — falta completar el nombre o el importe de alguna línea de costo, o el margen deseado.

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
