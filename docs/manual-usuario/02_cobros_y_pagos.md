# 2. Cobros y pagos

## ¿Para qué sirve?

Acá registrás la plata que efectivamente entra (cobros a clientes) o sale (pagos a proveedores), y la vinculás contra una o varias facturas ya confirmadas.

- Cobros: menú **Facturación → Cobros**.
- Pagos: menú **Facturación → Pagos**.

📷 *(Captura: pantalla de "Cobros" con el botón "Nuevo cobro")*

## Registrar un cobro

1. Hacé clic en **"Nuevo cobro"**.
2. Completá: **Cliente**, **Fecha**, **Moneda**, **Tipo de cambio** (lo cargás vos, el sistema no lo busca solo — si es en pesos, no hace falta tocarlo, queda en 1), **Cuenta bancaria** (a dónde entra la plata), **Total cobrado (bruto)**, y **Observaciones**.
3. En **"Imputación contra facturas"** (opcional), agregá una línea por cada factura que estás cancelando: elegís la factura (solo aparecen las ya confirmadas de ese cliente) y el monto que le aplicás. Si el cobro llega después de la fecha de vencimiento de la factura, el sistema te muestra automáticamente cuántos días de atraso tiene y calcula el recargo por mora si corresponde.
4. Si el cliente te retuvo algo (Ganancias o IVA), cargalo en **"Retenciones sufridas"**.
5. Los totales se actualizan solos: cuánto quedó imputado, cuánto queda como **anticipo** (la diferencia entre lo cobrado y lo imputado — si no imputás nada, todo el cobro queda como anticipo), cuánto se retuvo, y cuánto entra realmente a la cuenta bancaria.
6. **"Crear borrador"** y después **"Confirmar"** — igual que con las facturas, confirmar genera el asiento automáticamente.

## Registrar un pago

Es exactamente igual, pero eligiendo **Proveedor** y **facturas de compra** en vez de cliente/facturas de venta. No tiene la sección de retenciones (esas se cargan del lado de la factura de compra, en "Percepciones sufridas").

## ¿Qué pasa si cobro/pago en dólares con un tipo de cambio distinto al de la factura?

No tenés que calcular nada a mano. Si estás cancelando por completo una factura en USD y el tipo de cambio que cargaste en el cobro/pago es distinto al que tenía la factura original, **el sistema agrega automáticamente una línea de "diferencia de cambio"** al asiento — ganada si te favoreció, perdida si no. Vos solo tenés que cargar el tipo de cambio del día en que se hizo el cobro o el pago real; el resto es automático.

Esto solo se calcula quando terminás de saldar una factura por completo (no en pagos parciales intermedios).

## Anticipos

Si cobrás o pagás sin imputar contra ninguna factura (o imputás menos de lo cobrado/pagado), el resto queda como **anticipo** del cliente o proveedor. Más adelante, cuando tengas una factura confirmada para aplicarlo, entrá al cobro/pago original (tiene que estar confirmado y con anticipo disponible) y usá el botón **"Aplicar anticipo"**: elegís la factura, el monto y la fecha, y confirmás.

📷 *(Captura: formulario de "Aplicar anticipo" con factura, monto y fecha)*

## Anular un cobro o pago

Igual que las facturas: solo se anulan los ya confirmados, con motivo obligatorio. **No se puede anular un cobro o pago que ya tenga aplicaciones de anticipo registradas** — primero hay que resolver esas aplicaciones.

## Errores más comunes al cargar un cobro o pago

- **"La suma de las imputaciones no puede superar el total cobrado/pagado"** — cargaste más plata imputada contra facturas de la que realmente cobraste o pagaste. Revisá los montos de cada línea.
- **"La factura... no está confirmada"** — solo se puede imputar contra facturas ya confirmadas, no contra borradores.
- **"La factura... está en otra moneda distinta a la del cobro"** — no se puede imputar una factura en pesos contra un cobro en dólares (o viceversa).
- **"La imputación supera el saldo pendiente de la factura..."** — la factura ya estaba parcialmente cobrada/pagada y el monto que cargaste supera lo que le quedaba pendiente.
- **"El anticipo disponible (...) es menor al monto solicitado"** — estás tratando de aplicar más anticipo del que realmente quedó disponible en ese cobro/pago.
- **El botón "Confirmar" de la anulación queda gris** — falta escribir el motivo en el campo de texto.

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
