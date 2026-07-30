# Manual de usuario — Sistema de Gestión Contable Montanari Tech

Este manual está pensado para el equipo administrativo y para el contador. No hace falta saber de sistemas ni de programación para usarlo: cada capítulo explica una parte del sistema con lenguaje simple, paso a paso, con los botones y campos tal cual aparecen en pantalla.

## Cómo está organizado

1. [Facturas de venta y compra](01_facturas_venta_compra.md)
2. [Cobros y pagos](02_cobros_y_pagos.md)
3. [Resúmenes bancarios y bandeja de pendientes](03_resumenes_bancarios_y_bandeja.md)
4. [Conciliación bancaria](04_conciliacion_bancaria.md)
5. [Liquidación de IVA e IIBB](05_liquidacion_iva_e_iibb.md)
6. [Cierre de períodos contables](06_cierre_de_periodos.md)
7. [Calendario de vencimientos y alertas](07_vencimientos_y_alertas.md)
8. [Presupuestos de proyectos y rentabilidad](08_presupuestos_y_rentabilidad.md)
9. [Búsqueda global "Lupita"](09_busqueda_lupita.md)
10. [Pendientes administrativos](10_pendientes_administrativos.md)
11. [Exportaciones](11_exportaciones.md)
12. [Preguntas frecuentes: los 10 errores de carga más comunes](12_preguntas_frecuentes.md)

## Algunas ideas generales que se repiten en todo el sistema

Antes de entrar módulo por módulo, conviene tener claras estas cuatro ideas — se repiten todo el tiempo:

- **Borrador vs. Confirmado vs. Anulado.** Casi todo lo que cargás (facturas, cobros, pagos, liquidaciones de impuestos) pasa por estos tres estados. Mientras está en **borrador** podés editarlo o eliminarlo tranquilo — todavía no impactó en la contabilidad. Al **confirmar**, el sistema genera el asiento contable automáticamente — ya no se puede editar. Si te equivocaste, no se "deshace": se **anula** (con un motivo), y queda todo el historial a la vista. Nada se borra en silencio.
- **Todo lo que se importa (resúmenes bancarios) primero cae en una bandeja de revisión.** Nada de lo que subís desde un Excel o PDF de un banco impacta la contabilidad automáticamente — vos decidís, línea por línea, qué hacer con cada movimiento.
- **Los períodos se pueden cerrar.** Una vez que un mes está cerrado, no se puede cargar ni editar nada con fecha de ese mes (salvo que un administrador lo autorice explícitamente, con motivo). Esto es para que, una vez que el contador cerró un mes, nadie lo pise sin querer.
- **Multimoneda (ARS/USD).** El sistema maneja pesos y dólares en la misma pantalla. Vos siempre cargás el tipo de cambio a mano en el momento de la operación — el sistema no lo busca solo. Las diferencias que aparecen por usar un tipo de cambio distinto al de la factura original las calcula el sistema automáticamente; no hay que calcular nada a mano.

📷 *(Captura: pantalla de inicio del sistema con el menú lateral)*
