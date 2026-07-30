# 1. Facturas de venta y compra

## ¿Para qué sirve?

Acá cargás las facturas que la empresa emite (venta) y las que recibe de proveedores (compra). Al confirmar una factura, **el sistema genera el asiento contable automáticamente** — no hace falta saber de débitos y créditos, el sistema se encarga.

- Facturas de venta: menú **Facturación → Ventas**.
- Facturas de compra: menú **Facturación → Compras**.

📷 *(Captura: listado de "Facturas de venta" con la tabla y el botón "Nueva factura")*

## Cargar una factura de venta

1. Hacé clic en **"Nueva factura"**. Se abre el formulario "Nueva factura (borrador)".
2. Completá:
   - **Cliente**
   - **Proyecto** (opcional — si no corresponde, dejalo en "Sin proyecto")
   - **Fecha** y **Vencimiento**
   - **Tipo de comprobante**
   - **Punto de venta** (ej. `0001`) y **Número** (ej. `00000123`)
   - **Jurisdicción destino** (opcional — para IIBB)
   - **Moneda** y **Tipo de cambio**
   - **Observaciones**
3. Cargá una o más **líneas**: Descripción, Tipo (Gravado / No gravado / Exento), Importe neto, Alícuota de IVA (0, 2.5, 5, 10.5, 21 o 27%), Tipo de ingreso, y una cuenta contable si necesitás forzar una distinta a la automática. Usá **"Agregar línea"** para sumar más, y **"Quitar"** para sacar una.
4. Los totales (Neto, IVA, Total) se recalculan solos a medida que cargás.
5. Hacé clic en **"Crear borrador"**. Todavía no impactó en la contabilidad — podés seguir editando.

## Cargar una factura de compra

Es igual, pero con estas diferencias:
- Elegís **Proveedor** en vez de Cliente.
- Las líneas llevan un **Tipo de costo** en vez de Tipo de ingreso.
- Hay una sección aparte, **"Percepciones sufridas (opcional)"**, para cargar percepciones de IVA o de IIBB que te haya cobrado el proveedor — elegís el Tipo, la Jurisdicción (si aplica) y el Importe, y hacés clic en **"Agregar percepción"**.

📷 *(Captura: formulario de carga de una factura de compra, con la sección de percepciones sufridas)*

## Confirmar una factura

Con la factura en **borrador**, hacé clic en **"Confirmar"**. Esto:

- Genera el asiento contable automáticamente — no hay ningún paso manual más.
- Deja la factura en solo lectura (ya no se puede editar ni eliminar).
- Muestra el número de asiento generado en la columna "N° Asiento" del listado.

Si algo no cierra (por ejemplo, falta configurar una cuenta contable para algún concepto), el sistema **no** confirma la factura ni genera nada a medias — te avisa qué falta para poder solucionarlo antes de reintentar.

## Anular una factura

Solo se puede anular una factura **ya confirmada** (no una en borrador — esa se elimina directamente). Hacé clic en **"Anular"**, escribí el motivo en el campo que aparece, y confirmá. Queda registrada como anulada, con el motivo a la vista — no desaparece del listado.

## Adjuntar el comprobante

Una vez que editás una factura ya guardada, podés subir el PDF del comprobante real (factura escaneada o el PDF que te mandó el proveedor). Aparece en una lista con opción de descarga y de "Quitar".

## Buscar y filtrar

Arriba del listado hay un buscador de texto libre (por número o cliente/proveedor) y un filtro por Estado (Borrador / Confirmado / Anulado).

## Errores más comunes al cargar una factura

- **"La factura no tiene ningún importe a contabilizar"** — intentaste confirmar una factura sin líneas o con importes en cero. Revisá que las líneas tengan un importe neto mayor a cero.
- **"No hay una cuenta configurada para el concepto..."** — al confirmar, el sistema no encontró qué cuenta contable usar para algún concepto (por ejemplo, un tipo de costo nuevo). Hay que pedirle al administrador que complete el mapeo de cuentas antes de reintentar.
- **"Solo se pueden editar o eliminar facturas en borrador"** — estás intentando modificar una factura ya confirmada o anulada. Si la confirmaste por error, usá "Anular" con un motivo.
- **El período está cerrado** — si la fecha de la factura cae en un mes ya cerrado por el contador, el sistema te avisa. Solo un administrador puede autorizar la carga igual, cargando un motivo.

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
