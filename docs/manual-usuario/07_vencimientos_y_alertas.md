# 7. Calendario de vencimientos y alertas

## ¿Para qué sirve?

Acá se llevan todas las obligaciones de pago futuras de la empresa: impuestos, tarjeta, sueldos, suscripciones y cualquier otro compromiso con fecha. Además, el sistema muestra alertas automáticas cuando algo necesita atención.

Menú: **Presupuesto → Vencimientos**.

## Vistas disponibles

Arriba hay dos botones para cambiar de vista:
- **Calendario**: un mes a la vez, con los vencimientos como botones de color dentro de cada día. Hacé clic en el **"+"** de un día para cargar un vencimiento nuevo con esa fecha ya puesta, o hacé clic en un vencimiento existente para ver su detalle.
- **Lista**: una tabla con Fecha, Descripción, Tipo, Importe estimado, Estado, Recurrencia y Acciones, con filtros de fecha "Desde"/"Hasta".

📷 *(Captura: vista de calendario mensual con vencimientos marcados en distintos días)*

## Cargar un vencimiento nuevo

Hacé clic en **"Nuevo vencimiento"** y completá: Descripción, Tipo (IVA, IIBB, Ganancias, Bienes Personales, Cargas sociales, Sueldos, Contador, Tarjeta de crédito, Suscripción, Préstamo, Plan de pago, Pago automático, Otro), Fecha, Importe estimado, Moneda, Recurrencia (Única / Mensual / Anual / Personalizada — esta última pide cada cuántos días se repite), y opcionalmente Cuenta contable, Proveedor, Tarjeta de crédito, Proyecto, Concepto recurrente y Observaciones.

## Generar vencimientos automáticamente

El botón **"Generar ahora"** revisa 5 fuentes (liquidaciones de IVA/IIBB pendientes, resúmenes de tarjeta, conceptos recurrentes configurados, y vencimientos manuales recurrentes) y crea los que falten — sin duplicar los que ya existen. Podés usarlo las veces que quieras.

## Acciones sobre un vencimiento

- **Marcar pagado**
- **Reprogramar** (te pide la nueva fecha)
- **Cancelar** (te pide un motivo obligatorio)
- **Editar** — solo disponible mientras el vencimiento sigue Pendiente o fue Reprogramado (no se puede editar uno ya Pagado o Cancelado).

## Alertas

El ícono de campana en el encabezado del sistema muestra un número en rojo con la cantidad de alertas activas. Al hacer clic, se abre un panel con cada alerta y un botón **"Marcar leída"**.

Las alertas que el sistema genera automáticamente son:
- Un vencimiento próximo o ya vencido.
- Un compromiso de pago próximo.
- Una factura de compra próxima a vencer, o una factura de venta atrasada de cobro.
- El saldo de una cuenta bancaria por debajo de un mínimo configurado.
- Un movimiento bancario pendiente de revisar en la bandeja.
- Una diferencia en la conciliación bancaria.
- Un pendiente administrativo próximo a vencer.

Estas alertas se resuelven solas cuando la situación que las generó ya no aplica (por ejemplo, si pagás el vencimiento, la alerta desaparece).

📷 *(Captura: panel de alertas activas desplegado desde el ícono de campana)*

## Configurar las alertas

Desde el Dashboard, en la tarjeta "Configuración de alertas", se puede ajustar con cuántos días de anticipación avisar (por defecto 7) y a partir de cuántos días de atraso avisar por una factura de venta sin cobrar (por defecto 0, es decir, apenas se pasa la fecha).

## Errores más comunes

- **"Solo se puede editar un vencimiento Pendiente o Reprogramado"** — no se puede editar uno ya Pagado o Cancelado.
- **"El vencimiento ya está [Pagado/Cancelado]"** — estás intentando marcar pagado, reprogramar o cancelar algo que ya tiene ese estado final.

Ver más casos en [Preguntas frecuentes](12_preguntas_frecuentes.md).
