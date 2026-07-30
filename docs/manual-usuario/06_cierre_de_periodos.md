# 6. Cierre de períodos contables

## ¿Para qué sirve?

Permite "cerrar" un mes contable para que nadie pueda cargar, editar o anular nada con fecha de ese mes por error, una vez que el contador ya dio ese período por terminado. Consultar, importar y exportar información **nunca se bloquean** — el cierre solo afecta la carga y edición.

Menú: **Administración → Períodos** (solo visible para usuarios administradores).

## Paso a paso

1. Si es la primera vez, hacé clic en **"Generar períodos"** — el sistema crea automáticamente un período por cada mes que ya tiene asientos cargados, en estado **Abierto**.
2. En el listado, cada período tiene un botón según su estado:
   - **"Cerrar"** (si no está cerrado): pide un **motivo obligatorio**, y al confirmar bloquea la carga/edición de ese mes.
   - **"Marcar en revisión"** (solo si está Abierto): es un estado informativo, no cambia el bloqueo.
   - **"Reabrir"** (solo si está Cerrado): pide motivo y vuelve a habilitar la carga.
3. Un panel al costado muestra un resumen del período elegido: las liquidaciones de IVA/IIBB y el estado de la conciliación bancaria de ese mes.

📷 *(Captura: pantalla de "Períodos contables" con la lista de meses y sus estados)*

## ¿Qué pasa si intento cargar algo en un mes cerrado?

- Si sos un usuario de carga (no administrador): el sistema no te deja, con el mensaje *"El período está cerrado: solo un administrador puede modificarlo"*.
- Si sos administrador: aparece un aviso rojo con un campo para escribir el **motivo** y un botón **"Confirmar igual"** — el sistema te deja continuar, pero queda registrado por qué se hizo la excepción.

📷 *(Captura: aviso de "período cerrado" con el campo de motivo y el botón "Confirmar igual")*

## Errores más comunes

- **"Solo se puede reabrir un período cerrado"** — estás intentando reabrir uno que ya está Abierto o En revisión.
- **No aparece ningún período nuevo al generar** — es normal si ya existe un período para todos los meses con asientos; el sistema te avisa "No hay períodos nuevos para generar".

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
