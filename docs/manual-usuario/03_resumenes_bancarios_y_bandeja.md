# 3. Resúmenes bancarios y bandeja de pendientes

## ¿Para qué sirve?

Acá subís el resumen del banco (o de Mercado Pago, o de la tarjeta de crédito) y el sistema lee automáticamente cada movimiento. **Nada de esto impacta la contabilidad solo** — todo cae primero en una bandeja de revisión, y vos decidís qué hacer con cada línea.

- Importar un resumen: menú **Bancos → Importación**.
- Revisar lo importado: menú **Bancos → Movimientos**.

## Importar un resumen bancario

1. Entrá a **Bancos → Importación**.
2. Elegí el **Origen**:
   - **Banco Galicia** (home banking) — acepta Excel o PDF.
   - **Mercado Pago** (resumen de cuenta) — acepta Excel.
   - **Resumen de tarjeta de crédito** — acepta PDF.
3. Elegí la **Cuenta bancaria destino** y subí el **Archivo**.
4. Hacé clic en **"Previsualizar"**.
5. Se muestra una tabla para revisar antes de importar, con cada fila: si querés importarla (casilla), Fecha (la podés completar si el archivo no la trae), Descripción, Importe, Referencia y el Estado ("Nuevo" o "Ya importado" — esto último si el sistema detecta que esa misma línea ya se cargó antes, para que no dupliques).
   - Si hay filas en dólares, vas a tener que cargar un **tipo de cambio** para esas filas antes de poder confirmar.
   - Si el archivo no trae fecha en algunas filas, aparece un aviso: podés completarla ahí mismo o dejarla en blanco y completarla después desde la bandeja.
   - Con el resumen de tarjeta: trae tanto los pagos que mueven la cuenta bancaria como el detalle de consumos del período — desmarcá las filas que no correspondan importar en esta pantalla.
6. Hacé clic en **"Confirmar importación"**. Vas a ver un resultado por cada fila: **Importado**, **Ya importado anteriormente**, o **Error** (con el motivo).

📷 *(Captura: pantalla de previsualización de un resumen de Galicia antes de confirmar la importación)*

## La bandeja de movimientos bancarios

Todo lo que importás (o cargás a mano) entra acá con el estado **Pendiente**. Nada de esto tocó la contabilidad todavía.

Para cada movimiento pendiente, tenés estas acciones:

- **Editar**: corrige fecha, importe, descripción, etc.
- **Confirmar**: usa la cuenta contable que el sistema ya sugirió (por ejemplo, "comisión bancaria" si detectó esa palabra en la descripción) y genera el asiento automáticamente. Necesita que el movimiento tenga fecha y una sugerencia cargada.
- **Imputar**: elegís vos mismo la cuenta contable (por si el sistema no sugirió nada, o sugirió mal) y genera el asiento.
- **Asociar**: en vez de generar un asiento nuevo, vinculás el movimiento a un asiento que ya existe y está confirmado (por ejemplo, si ya cargaste esa operación por otro lado). Necesitás el número de ese asiento.
- **Descartar**: si el movimiento no corresponde registrarlo (por ejemplo, un duplicado del banco), lo descartás con un motivo obligatorio. No genera ningún asiento.

Una vez que confirmás, imputás o asociás un movimiento, pasa a estado **Conciliado** y muestra el número de asiento generado. Si lo descartás, pasa a **Descartado** con el motivo a la vista. Ninguno de los dos estados se puede reabrir — es definitivo.

📷 *(Captura: bandeja de movimientos bancarios, con un movimiento pendiente y sus botones de acción)*

## Cargar un movimiento a mano

Si necesitás cargar algo que no vino de ningún resumen (por ejemplo, algo que te comunicó el banco por otro medio), usá **"Nuevo movimiento"** en la bandeja y completá los mismos datos: cuenta bancaria, fecha, descripción, importe, moneda, tipo de cambio, referencia, y opcionalmente una cuenta sugerida.

## Bancos y formatos que reconoce el sistema

| Origen | Formato |
|---|---|
| Banco Galicia | Excel (.xlsx) o PDF |
| Mercado Pago | Excel (.xlsx) |
| Tarjeta de crédito (VISA Business Banco Galicia) | PDF |

## Errores más comunes al importar un resumen

- **"El archivo no tiene encabezado reconocible" / "No se reconocen las columnas esperadas"** — el Excel no tiene el formato esperado del banco (por ejemplo, se borró o movió una fila de encabezado). Volvé a descargar el resumen original del banco sin modificarlo.
- **"No se pudo leer el resumen de..."** — el archivo está dañado, es de otro banco, o es un formato distinto al esperado (por ejemplo, subiste un PDF donde se esperaba un Excel).
- **"Falta el tipo de cambio para importar filas en USD"** — hay líneas en dólares en el archivo y no cargaste la cotización antes de confirmar.
- **Fila marcada como "Ya importado anteriormente"** — el sistema detectó que esa combinación de cuenta + fecha + importe + descripción ya se había importado antes. Es normal si volvés a subir el mismo resumen por error; no se duplica.
- **"Este movimiento no tiene una cuenta sugerida — usá 'imputar' para elegir una cuenta"** — al hacer clic en "Confirmar" sin que haya una sugerencia cargada. Usá "Imputar" en su lugar y elegí la cuenta a mano.
- **"Este movimiento no tiene fecha cargada"** — hay que completar la fecha con "Editar" antes de poder confirmar o imputar.

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
