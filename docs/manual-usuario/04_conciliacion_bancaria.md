# 4. Conciliación bancaria

## ¿Para qué sirve?

Es la pantalla donde comparás el saldo del banco contra el saldo que tiene el sistema, y el sistema te va sugiriendo qué movimientos coinciden con qué asientos — vos siempre decidís si aceptar cada sugerencia. Para cargar, corregir o descartar un movimiento a mano, se usa la [bandeja de movimientos bancarios](03_resumenes_bancarios_y_bandeja.md); esta pantalla es solo para revisar y aceptar sugerencias.

Menú: **Bancos → Conciliación**.

## Paso a paso

1. Elegí la **Cuenta bancaria** que querés conciliar.
2. Ajustá el rango de fechas (**Desde** / **Hasta** — por defecto, el mes en curso) y la **Tolerancia (días)** — cuántos días de diferencia entre fechas se aceptan para considerar que dos movimientos son "el mismo".
3. Arriba vas a ver un resumen con 4 datos: **Saldo banco**, **Saldo sistema**, **Diferencia** (en verde si están iguales, en rojo si no) y cuántas **sugerencias** hay disponibles para revisar.
4. Abajo, la tabla de movimientos del período muestra, para cada uno, si el sistema encontró una sugerencia:
   - **Match con un asiento existente**: te muestra el número de asiento sugerido — botones **"Aceptar"** o **"Rechazar"**.
   - **Sugerencia de cuenta contable** (por ejemplo, detectó la palabra "comisión" en la descripción y sugiere la cuenta de comisiones bancarias): botones **"Aplicar"** o **"Rechazar"**.
   - **Sin sugerencia**: te indica que hay que resolverlo desde la bandeja de movimientos bancarios.
5. Si aceptás o aplicás una sugerencia, el sistema genera o vincula el asiento correspondiente automáticamente — el movimiento pasa a Conciliado en la bandeja. "Rechazar" solo la oculta de esta pantalla por ahora; no descarta el movimiento (para eso, andá a la bandeja).

📷 *(Captura: pantalla de conciliación con el resumen de saldo banco vs. saldo sistema y la tabla de sugerencias)*

## Errores más comunes

- **"La fecha desde no puede ser posterior a la fecha hasta"** — revisá el rango de fechas elegido.
- **La diferencia entre saldo banco y saldo sistema no cierra en cero** — es normal si todavía quedan movimientos pendientes en la bandeja sin conciliar. Revisalos ahí antes de dar por cerrado el mes.

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
