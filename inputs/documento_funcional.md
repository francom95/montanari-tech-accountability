# Documento funcional fuente — Sistema de Gestión Contable para Montanari Tech

> Secciones 1 a 19. Entrada obligatoria de F1.1 y referencia para todos los pasos del plan.

## 1. Descripción general del proyecto

El proyecto consiste en desarrollar un sistema web de gestión contable, financiera, impositiva y operativa para Montanari Tech, con el objetivo de reemplazar progresivamente el Excel actual de contabilidad por una plataforma centralizada, ordenada, segura y automatizada.

El sistema estará orientado inicialmente al uso interno de Montanari Tech, pero deberá estar diseñado con una arquitectura suficientemente flexible para que, en el futuro, pueda transformarse en un producto utilizable por otras empresas.

En esta primera etapa no será necesario manejar multiempresa desde la interfaz, ya que el foco será exclusivamente Montanari Tech. Sin embargo, se recomienda no construir la lógica de forma rígida o excesivamente atada a una única empresa, para facilitar una futura evolución del sistema como producto.

El sistema no tendrá como objetivo principal emitir facturas reales desde ARCA/AFIP. En esta etapa deberá registrar facturas ya emitidas, tanto de ventas como de compras, y utilizarlas para alimentar la contabilidad, los reportes, el control de cobros, el control de pagos y los impuestos.

La contabilidad será principalmente de gestión interna, orientada al control financiero y al apoyo del contador. No necesariamente deberá reemplazar el trabajo contable legal ni generar balances formales listos para presentación, pero sí deberá permitir tener información ordenada, trazable y confiable para tomar decisiones y facilitar el trabajo administrativo-contable.

El sistema deberá permitir cargar información una sola vez y reutilizarla en los distintos módulos. Por ejemplo, una factura de venta deberá generar una cuenta a cobrar, IVA débito fiscal, ingreso por ventas u otros ingresos por ventas, y además alimentar reportes de ventas, IVA, IIBB, estado de resultados, control de cobranzas, flujo de caja y reporte por proyecto.

Del mismo modo, una factura de compra deberá generar costo o gasto, IVA crédito fiscal si corresponde y cuentas por pagar. Además, deberá alimentar reportes de proveedores, IVA, flujo de caja, estado de resultados y reporte por proyecto.

El objetivo principal del sistema será que Montanari Tech pueda conocer de forma rápida y confiable:

- cuánto vendió;
- cuánto cobró;
- cuánto tiene por cobrar;
- cuánto debe pagar;
- qué impuestos tiene pendientes;
- cuál es su resultado mensual;
- cuál es la rentabilidad de cada proyecto;
- cómo está su caja;
- qué compromisos futuros tiene;
- qué movimientos están pendientes de revisión;
- y qué tareas administrativas quedan por resolver.

## 2. Módulos principales del sistema

El sistema deberá organizarse en los siguientes módulos principales:

1. Gestión Operativa y Maestros
2. Contabilidad
3. Facturación, Cobros y Pagos
4. Bancos, Tarjetas y Conciliaciones
5. Reportes
6. Impuestos
7. Presupuesto, Vencimientos y Proyección de Caja
8. Pendientes Administrativos
9. Seguridad, Usuarios y Auditoría

El módulo de Gestión Operativa y Maestros será clave porque allí se crearán los datos base del sistema: clientes, proveedores, proyectos, etapas de proyectos, presupuestos estimados, comisionistas, cuentas bancarias, tarjetas, monedas, tipos de cambio, categorías contables, rubros, conceptos recurrentes, costos y vencimientos.

Luego, esos datos serán utilizados por facturación, contabilidad, bancos, impuestos, reportes, vencimientos y conciliaciones.

## 3. Módulo de Gestión Operativa y Maestros

### 3.1. Descripción general del módulo

Este módulo será la base administrativa del sistema. Deberá permitir crear, editar, listar, buscar, activar, desactivar y eliminar, cuando corresponda, los datos principales que luego serán utilizados por el resto de la plataforma.

En este módulo se deberán crear los proyectos. Los proyectos no deberían crearse directamente dentro de facturación, porque un proyecto puede existir antes de facturarse, puede tener etapas, presupuesto estimado, pagos pactados, proveedores asociados, comisiones, costos, cobros esperados, vencimientos, impuestos asociados y reportes propios.

Facturación deberá consumir los proyectos ya creados en este módulo.

### 3.2. Gestión de proyectos

Cada proyecto deberá tener, como mínimo, los siguientes datos: nombre del proyecto; cliente asociado; responsable; país; tipo de proyecto; estado; moneda; monto total; cantidad de pagos pactados; fechas estimadas de cobro; importes por cuota; comentarios; estado comercial; estado de facturación; estado de cobranza; fecha estimada de finalización.

El sistema deberá permitir que cada proyecto tenga una o varias etapas. Cada etapa del proyecto deberá poder cargarse manualmente o importarse desde un archivo, contemplando que puede haber varias etapas en curso al mismo tiempo.

Cada etapa deberá tener: nombre; descripción; estado; fecha de inicio; fecha estimada de finalización; porcentaje de avance; monto presupuestado; costos estimados; proveedores asociados; pagos previstos; cobros previstos; observaciones.

La creación o modificación de proyectos deberá permitir cargar un presupuesto estimado. Ese presupuesto deberá poder armarse siguiendo el modelo de hoja y fórmulas que será incorporado como referencia.

El sistema deberá tomar ese modelo como base para calcular: costos; márgenes; impuestos; comisiones; rentabilidad estimada; cuotas de cobro; pagos a proveedores; resultado esperado del proyecto.

El presupuesto estimado del proyecto deberá permitir comparar lo proyectado contra lo real. De esta forma, el sistema deberá mostrar: cuánto se presupuestó cobrar; cuánto se cobró realmente; cuánto se presupuestó pagar a proveedores; cuánto se pagó realmente; qué impuestos se estimaron; qué impuestos impactaron; qué margen se esperaba; qué margen final tuvo el proyecto; qué diferencias existen entre lo presupuestado y lo ejecutado.

### 3.3. Gestión de clientes

El sistema deberá permitir administrar clientes. Cada cliente deberá contar con: nombre comercial; razón social; CUIT; domicilio; país; provincia o jurisdicción; email; teléfono; condición frente al IVA; tipo de persona; tipo de cliente; proyectos asociados; observaciones.

El sistema deberá permitir listar, buscar, editar, activar, desactivar o eliminar clientes, siempre que no existan restricciones por movimientos asociados.

### 3.4. Gestión de proveedores

El sistema deberá permitir administrar proveedores. Cada proveedor deberá tener: nombre; razón social; CUIT; domicilio; datos de facturación; condición frente al IVA; email; teléfono; tipo de proveedor; servicios que presta; moneda habitual de pago; proyectos asignados; pagos pactados; observaciones.

Los proveedores deberán poder clasificarse por tipo de servicio, por ejemplo: diseño; programación; edición; administración; contador; comisiones; suscripciones; impuestos; servicios profesionales; infraestructura; software; otros costos.

Además, el sistema deberá permitir agregar o especificar nuevos tipos de costos, para que un proveedor pueda asociarse a un costo existente o a un nuevo costo creado por el usuario.

### 3.5. Gestión de comisionistas

El sistema deberá permitir administrar comisionistas. Cada comisionista deberá poder vincularse con uno o más proyectos y definir: porcentaje de comisión; base de cálculo; moneda; importe estimado; importe final; estado de pago; fecha estimada de pago; observaciones.

Las comisiones deberán alimentar la rentabilidad por proyecto, las cuentas por pagar, el presupuesto de pagos y el flujo de caja proyectado.

### 3.6. Otros maestros del sistema

También deberán administrarse: cuentas bancarias; cuentas de dinero; tarjetas de crédito; monedas; tipos de cambio; jurisdicciones impositivas; categorías contables; rubros; conceptos recurrentes; costos; vencimientos; fórmulas o parámetros utilizados para presupuestar proyectos.

## 4. Módulo de Contabilidad

### 4.1. Descripción general del módulo

El módulo de contabilidad será el núcleo del sistema. Deberá permitir registrar y consultar la información contable de Montanari Tech de forma ordenada, trazable y flexible. El sistema deberá permitir administrar el plan de cuentas, registrar asientos manuales y automáticos, consultar mayores contables, validar saldos, controlar movimientos y alimentar reportes.

### 4.2. Plan de cuentas

El sistema deberá permitir administrar el plan de cuentas. El plan de cuentas deberá tener estructura jerárquica, con cuentas madre y cuentas imputables. Las cuentas madre servirán únicamente para agrupar información y no deberán permitir carga directa de movimientos. Las cuentas imputables serán las únicas que podrán utilizarse en asientos contables.

Cada cuenta deberá tener: código único; nombre; tipo de cuenta; rubro; tipo de saldo esperado; estado activo/inactivo; proyecto o proyectos en los cuales se utiliza habitualmente. Esto permitirá saber qué cuentas se usan para determinados proyectos, clientes, costos, ingresos o movimientos recurrentes.

El plan de cuentas deberá contemplar como mínimo las siguientes grandes categorías: Activo; Pasivo; Patrimonio Neto; Resultado Positivo; Resultado Negativo. Además, deberá permitir crear nuevas categorías contables, nuevos rubros y nuevas cuentas, para que el sistema no quede limitado al plan inicial.

Dentro de las cuentas deberán existir rubros como: Caja y Bancos; Créditos por Ventas; Otros Créditos; Inversiones Transitorias; Deudas Comerciales; Deudas Sociales; Deudas Fiscales; Deudas Bancarias; Otras Deudas; Ingresos por Ventas; Otros Ingresos por Ventas; Costos de Prestación de Servicios; Gastos de Comercialización; Gastos de Administración; Gastos Financieros; Impuestos; Comisiones; Suscripciones; Intereses.

### 4.3. Asientos contables

El sistema deberá permitir crear asientos contables manuales. Cada asiento deberá tener: fecha; número interno de asiento; descripción o leyenda; cuentas involucradas; importes en debe y haber; proyecto asociado cuando corresponda; cliente asociado cuando corresponda; proveedor asociado cuando corresponda; destino de fondos cuando corresponda; observaciones cuando corresponda; origen del asiento cuando corresponda.

El número interno de asiento no reemplaza a la descripción ni al código de las cuentas. Sirve para identificar y agrupar todas las líneas que pertenecen a una misma operación contable. Por ejemplo, un mismo asiento puede tener varias líneas con distintas cuentas en debe y haber, pero todas comparten el mismo número interno. Este número debería generarse automáticamente por el sistema y servir para búsqueda, auditoría y trazabilidad.

El sistema no deberá limitar la carga de asientos por orden cronológico estricto. Deberá permitir crear un asiento con una fecha intermedia aunque ya existan asientos con fechas anteriores y posteriores. Por ejemplo, si ya existen movimientos del 10/06 y del 20/06, el sistema deberá permitir cargar luego un asiento con fecha 16/06 sin generar inconvenientes. El ordenamiento contable y los reportes deberán basarse en la fecha del asiento, no necesariamente en el orden de carga.

Todo asiento deberá balancear. La suma del debe deberá ser igual a la suma del haber. Si el asiento no balancea, el sistema no deberá permitir confirmarlo. Podría permitirse guardarlo como borrador, pero no como asiento definitivo.

### 4.4. Edición, duplicación y eliminación de asientos

Los asientos deberán poder: buscarse; visualizarse; duplicarse; editarse; eliminarse; anularse; según permisos.

Los asientos automáticos generados desde facturación, impuestos, pagos o cobros deberán poder editarse manualmente después de generados. Toda edición deberá quedar registrada en auditoría.

La duplicación de asientos será importante para operaciones repetitivas. El sistema deberá permitir duplicar un asiento existente y modificar fecha, descripción, cuentas, importes, proyecto o cualquier dato necesario.

### 4.5. Mayores contables

El sistema deberá incluir mayores contables por cuenta. Desde cada cuenta del plan de cuentas se deberá poder ver el detalle de todos los movimientos que la componen, con: fecha; número de asiento; descripción; debe; haber; saldo acumulado; saldo final. El sistema deberá indicar si el saldo final es deudor o acreedor.

Los mayores deberán poder filtrarse por: fecha; período; cuenta; rubro; proyecto; cliente; proveedor; tipo de operación; moneda; origen del movimiento.

## 5. Módulo de Facturación, Cobros y Pagos

### 5.1. Descripción general del módulo

Este módulo deberá permitir registrar facturas ya emitidas y facturas recibidas. No deberá emitir facturas reales ante ARCA/AFIP en la primera etapa. También deberá permitir registrar cobros, pagos, pagos parciales, cobros parciales, anticipos, saldos pendientes, vencimientos y estados.

### 5.2. Facturas de venta

Las facturas de venta deberán cargarse con: fecha; cliente; proyecto asociado; número de comprobante; tipo de factura; jurisdicción de destino; moneda; tipo de cambio; detalle de servicios; base imponible; alícuota de IVA; IVA calculado; percepciones o retenciones si correspondieran; importe total; estado de facturación; estado de cobro.

Al guardar una factura de venta, el sistema deberá generar automáticamente el asiento contable correspondiente. Ese asiento deberá registrar, como mínimo: cuenta a cobrar en el activo; IVA débito fiscal en el pasivo; ingreso por ventas u otros ingresos por ventas como resultado positivo, según corresponda.

En caso de que corresponda, el asiento de ventas deberá poder modificarse manualmente, permitiendo agregar cuentas que no necesariamente afecten a la factura. Esto será útil para ajustes, reclasificaciones, diferencias o movimientos contables asociados que deban quedar registrados sin alterar el comprobante original.

La factura de venta también deberá alimentar: reportes de ventas; estado de resultados; IVA; IIBB; control de cuentas por cobrar; flujo de caja; reporte detallado por proyecto.

### 5.3. Facturas de compra

Las facturas de compra o facturas de proveedores deberán cargarse con: fecha; proveedor; proyecto asociado si corresponde; número de factura; descripción del servicio o compra; categoría contable; moneda; tipo de cambio; base imponible; IVA crédito fiscal; percepciones; retenciones; importe total; estado de pago.

Al guardar una factura de compra, el sistema deberá generar automáticamente: costo o gasto correspondiente; IVA crédito fiscal si corresponde; deuda comercial o cuenta por pagar.

También deberá alimentar: reportes de cuentas a pagar; costos; proveedores; IVA crédito fiscal; flujo de caja; estado de resultados; reporte detallado por proyecto.

### 5.4. Adjuntos

Las facturas de venta y compra podrán tener adjunto el PDF del comprobante, pero este adjunto será opcional, no obligatorio.

### 5.5. Diferencia entre factura, cobro y pago

El sistema deberá distinguir claramente entre factura, cobro y pago. Una factura de venta genera una cuenta a cobrar, pero el cobro puede ocurrir en otro momento. Una factura de proveedor genera una cuenta a pagar, pero el pago puede realizarse después.

### 5.6. Cobros y pagos

El sistema deberá permitir: cobros parciales; pagos parciales; anticipos; pagos a cuenta; saldos pendientes; vencimientos; estados; cobros por Mercado Pago; transferencias bancarias; caja; cuentas en dólares; otros medios de pago.

También deberá permitir cobros y pagos en distintas monedas. Cuando el cobro o pago sea en una moneda distinta de ARS, deberá tener un tipo de cambio asociado. En los asientos y reportes, el sistema deberá reflejar el importe convertido a moneda argentina, sin perder el dato de la moneda original. El sistema deberá contemplar diferencias de cambio cuando corresponda.

En cuentas por cobrar deberá poder verse: cliente; proyecto; factura; fecha; vencimiento; importe; moneda; cobros parciales; saldo pendiente; estado.

En cuentas por pagar deberá poder verse: proveedor; factura; fecha; vencimiento; importe; moneda; pagos parciales; saldo pendiente; estado.

### 5.7. Estados

El sistema deberá manejar estados para facturas, pagos, cobros y asientos. Como mínimo, deberán existir los siguientes estados: borrador; confirmado; anulado. Esto permitirá diferenciar registros en proceso de carga, registros válidos y registros anulados sin perder trazabilidad.

### 5.8. Importación de facturación histórica

Deberá existir la opción de importar facturaciones anteriores mediante Excel, CSV o PDF. Esta funcionalidad permitirá cargar comprobantes históricos y cruzarlos posteriormente con movimientos bancarios, pagos, cobros y conciliaciones.

## 6. Módulo de Bancos, Tarjetas y Conciliaciones

### 6.1. Descripción general del módulo

Este módulo deberá administrar: cuentas bancarias; cuentas de dinero; Mercado Pago; tarjetas de crédito; movimientos financieros; importaciones bancarias; conciliaciones.

### 6.2. Cuentas bancarias y cuentas de dinero

Deberán poder crearse cuentas en pesos y dólares, como: Banco Galicia CC; Banco Galicia USD; Mercado Pago; otras cuentas que se agreguen en el futuro.

Cada cuenta deberá tener: moneda; banco; alias o identificación; saldo inicial; movimientos asociados; estado de conciliación.

El sistema deberá permitir definir un saldo inicial para cada cuenta bancaria, cuenta de dinero o tarjeta. En el saldo inicial se tendrá que colocar la fecha del saldo que aparece en el banco para que en un futuro si se desea cargar la información histórica se pueda realizar y modificar el saldo inicial y su fecha. Esto es importante para poder comenzar a utilizar el sistema desde un punto determinado, sin necesidad de registrar inicialmente todos los movimientos desde la creación de la sociedad. A partir de ese saldo inicial, el sistema podrá calcular la evolución posterior de cada cuenta.

### 6.3. Importación de resúmenes

En la primera etapa se deberá contemplar la importación de resúmenes bancarios reales desde Galicia, Mercado Pago y tarjetas. Los formatos esperados podrán ser Excel, CSV o PDF, según lo que permita cada entidad. El objetivo será cruzar movimientos bancarios contra facturas, cobros, pagos y asientos contables.

La importación bancaria no deberá crear movimientos definitivos automáticamente. Primero deberá dejar los movimientos importados como pendientes de revisar. Luego, el usuario podrá: confirmar; asociar; imputar; descartar; corregir; cada movimiento antes de que impacte definitivamente en la contabilidad.

### 6.4. Conciliación bancaria

El sistema deberá permitir conciliación bancaria. La conciliación deberá comparar movimientos del banco contra movimientos del sistema. Deberá permitir: marcar movimientos conciliados; detectar diferencias; identificar cobros no asignados; identificar pagos no registrados; identificar comisiones bancarias; identificar impuestos bancarios; identificar SIRCREB; identificar percepciones; identificar débitos; identificar créditos; identificar movimientos pendientes de imputación.

### 6.5. Tarjetas de crédito

El sistema deberá incluir gestión de tarjetas de crédito desde la primera etapa. Cada tarjeta deberá tener: entidad; moneda; fecha de cierre; fecha de vencimiento; resumen; consumos; impuestos; intereses; pagos realizados; saldo pendiente; conciliación con cuentas bancarias.

Los consumos de tarjeta deberán poder clasificarse contablemente y asociarse a: proveedores; proyectos; suscripciones; impuestos; otros gastos. El pago del resumen deberá generar o vincularse con el asiento correspondiente.

## 7. Módulo de Reportes

### 7.1. Descripción general del módulo

El módulo de reportes deberá permitir visualizar la información contable, financiera, comercial, impositiva y operativa de forma clara y exportable.

### 7.2. Reportes imprescindibles para la primera versión

Para la primera versión, los reportes imprescindibles serán: Mayores Contables; Estado de Resultados; Balance de Sumas y Saldos; Tablero de Dashboard; Control de Pagos y Cobros; Reporte Detallado por Proyecto.

### 7.3. Dashboard

El dashboard deberá mostrar: ventas del período; cobros realizados; cuentas por cobrar; cuentas por pagar; saldo de caja; saldo por banco; impuestos próximos a vencer; resultado mensual; margen estimado; egresos proyectados; obligaciones próximas; alertas importantes.

### 7.4. Balance de Sumas y Saldos

El Balance de Sumas y Saldos deberá mostrar todas las cuentas contables con: total debe; total haber; saldo. Deberá permitir verificar si la contabilidad está balanceada y detectar diferencias o errores. Desde cada línea debería poder abrirse el mayor de la cuenta correspondiente.

### 7.5. Estado de Resultados

El Estado de Resultados deberá mostrar: ingresos por ventas; otros ingresos por ventas; costos de prestación de servicios; gastos de comercialización; gastos administrativos; gastos financieros; impuestos; otros ingresos; otros egresos.

Deberá poder visualizarse por: mes; año; acumulado; proyecto, cuando corresponda.

### 7.6. Reporte Detallado por Proyecto

El sistema deberá incluir un Reporte Detallado por Proyecto. Este reporte permitirá filtrar por proyecto y visualizar la información económica, financiera y operativa de cada trabajo.

Deberá mostrar: nombre del proyecto; cliente asociado; etapas del proyecto; tiempo del proyecto; fecha de inicio; fecha estimada de finalización; fecha real de finalización, si corresponde; estado del proyecto; total presupuestado; total facturado; total cobrado; total pendiente de cobro; cuotas cobradas; cuotas pendientes de cobro; proveedores que trabajan o trabajaron en el proyecto; total presupuestado a pagar a proveedores; total pagado a proveedores; total pendiente de pago; cuotas pagadas; cuotas pendientes de pago; comisionista asociado, si existe; comisión calculada; impuestos asociados; costos; gastos; margen estimado; margen real; diferencia entre presupuesto y resultado real; rentabilidad final.

Este reporte será clave para medir la rentabilidad por proyecto. Deberá permitir conocer ingresos menos costos, pagos a proveedores, comisiones, impuestos asociados y otros gastos vinculados. El objetivo será saber si cada proyecto fue rentable, cuánto margen dejó, qué obligaciones quedan pendientes y qué diferencias hubo contra el presupuesto original.

### 7.7. Estado de Situación Patrimonial

El sistema también deberá permitir generar Estado de Situación Patrimonial mensual, aunque podría quedar para una etapa posterior si se prioriza el MVP. Este reporte deberá mostrar: Activo; Pasivo; Patrimonio Neto; agrupado por rubros contables.

### 7.8. Flujo de caja real y proyectado

El flujo de caja real y proyectado deberá mostrar: saldo inicial; ingresos cobrados; egresos pagados; movimientos de inversión; movimientos de financiación; saldo final.

El flujo proyectado deberá contemplar: cobros esperados; pagos comprometidos; impuestos próximos; sueldos; cargas sociales; honorarios; comisiones; suscripciones; tarjetas; vencimientos de IVA diferido; IIBB; anticipos de Ganancias; otros pagos recurrentes.

### 7.9. Exportación de reportes

Todos los reportes relevantes deberán tener exportación a Excel y PDF. Especialmente: mayores; balance de sumas y saldos; estado de resultados; reporte por proyecto; cuentas por cobrar; cuentas por pagar; IVA; IIBB; flujo de caja; clientes; proveedores; movimientos contables.

## 8. Módulo de Impuestos

### 8.1. Descripción general del módulo

El módulo de impuestos deberá permitir calcular IVA a pagar e IIBB a pagar de forma editable, trazable y conectada con la contabilidad.

### 8.2. IVA

El cálculo de IVA deberá nutrirse de: facturas de venta; facturas de compra; asientos contables; comisiones bancarias; percepciones de IVA; saldos técnicos anteriores.

El sistema deberá mostrar: IVA débito fiscal; IVA crédito fiscal; percepciones; restituciones; saldo técnico; saldo a favor; saldo a pagar.

Al confirmar el cálculo mensual de IVA, el sistema deberá generar automáticamente el asiento contable correspondiente. Ese asiento deberá impactar en el pasivo fiscal, en el saldo a favor o en la cuenta que corresponda según el resultado del cálculo.

### 8.3. IIBB

El cálculo de IIBB deberá contemplar: jurisdicciones; actividad; alícuotas; base imponible; impuesto determinado; saldos a favor anteriores; retenciones; percepciones; SIRCREB; pagos a cuenta.

Deberán poder configurarse jurisdicciones como: Buenos Aires; CABA; Córdoba; otras jurisdicciones. Cada jurisdicción deberá poder tener sus códigos y alícuotas correspondientes.

Al confirmar el cálculo mensual de IIBB, el sistema deberá generar automáticamente el asiento contable correspondiente y actualizar el pasivo fiscal o saldo a favor.

### 8.4. Ajustes impositivos

Los cálculos impositivos deberán poder editarse antes de confirmarse, ya que pueden existir ajustes, percepciones, retenciones, saldos anteriores o criterios específicos que deban agregarse o corregirse manualmente.

Los impuestos también deberán poder asociarse a proyectos cuando corresponda, especialmente para alimentar el reporte detallado por proyecto y calcular correctamente la rentabilidad de cada trabajo.

## 9. Módulo de Presupuesto, Vencimientos y Proyección de Caja

### 9.1. Descripción general del módulo

Este módulo deberá permitir administrar compromisos futuros, vencimientos, pagos previstos, impuestos, obligaciones recurrentes y proyección de caja.

### 9.2. Calendario de vencimientos

El sistema deberá incluir un calendario de vencimientos. Este calendario deberá mostrar: vencimientos impositivos; planes de pago; IVA; IIBB; Ganancias; Bienes Personales Acciones y Participaciones; cargas sociales; sueldos; contador; tarjetas; suscripciones; préstamos; pagos automáticos; cualquier obligación recurrente.

Cada vencimiento deberá poder marcarse como: pendiente; pagado; vencido; reprogramado; cancelado.

También deberá poder asociarse a: cuenta contable; proveedor; impuesto; tarjeta; proyecto; concepto.

### 9.3. Presupuesto de pagos

El presupuesto de pagos deberá permitir registrar: compromisos futuros; cuotas de planes de pago; vencimientos impositivos; IVA diferido; IIBB; pagos a proveedores; sueldos; cargas sociales; contador; comisiones bancarias; comisiones por ventas; suscripciones; tarjetas; otros egresos esperados.

Estos compromisos deberán alimentar automáticamente el flujo de caja proyectado.

### 9.4. Inversiones

El sistema deberá contemplar inversiones en Fondos Fima u otros instrumentos similares. Deberá permitir registrar: suscripciones; rescates; valuaciones; cuotapartes; valor de cuotaparte; monto aplicado; rendimiento; fecha de liquidación; objetivo del dinero invertido; relación con futuros pagos.

Esto será importante para administrar dinero destinado a impuestos u obligaciones futuras sin dejarlo inmovilizado.

## 10. Módulo de Pendientes Administrativos

### 10.1. Descripción general del módulo

El sistema deberá incluir una pantalla específica de pendientes administrativos, similar a la lógica utilizada actualmente en el Excel. Esta pantalla deberá permitir cargar tareas administrativas sueltas que no necesariamente sean una factura, un pago, un cobro, un asiento o un impuesto, pero que deban ser recordadas, revisadas o resueltas.

### 10.2. Datos de cada pendiente

Cada pendiente deberá tener: título; descripción; fecha de creación; fecha estimada de resolución; prioridad; estado; responsable; categoría; proyecto asociado si corresponde; cliente asociado si corresponde; proveedor asociado si corresponde; observaciones.

Los estados posibles podrían ser: pendiente; en proceso; resuelto; cancelado; postergado.

Esta pantalla deberá servir para centralizar: recordatorios administrativos; controles manuales; revisiones del contador; ajustes pendientes; facturas a pedir; pagos a verificar; movimientos bancarios a identificar; impuestos a revisar; cualquier tarea operativa vinculada a la gestión contable y financiera.

## 11. Monedas y Tipo de Cambio

El sistema deberá manejar principalmente ARS y USD desde la primera etapa.

Cada operación deberá registrar: moneda original; tipo de cambio utilizado; fuente del tipo de cambio; importe en moneda original; importe convertido a pesos argentinos; posible diferencia de cambio.

El tipo de cambio deberá ser configurable. El sistema deberá permitir definir criterios como: dólar BNA venta; dólar BNA compra; dólar oficial del día; tipo de cambio manual por operación; otro criterio definido por Montanari Tech.

Cuando existan facturas, cobros o pagos en USD y registros contables en ARS, el sistema deberá permitir calcular diferencias de cambio cuando corresponda. Los reportes y asientos deberán poder expresarse en moneda argentina, sin perder la trazabilidad de la moneda original y del tipo de cambio utilizado.

## 12. Alertas y Notificaciones

El sistema deberá incluir alertas automáticas desde la primera etapa. Las alertas deberán contemplar: vencimientos impositivos próximos; pagos pendientes; facturas sin cobrar; saldos bajos; vencimientos de tarjetas; impuestos a pagar; obligaciones vencidas; cobros atrasados; pagos próximos a proveedores; vencimientos de planes de pago; movimientos bancarios pendientes de revisar; diferencias detectadas en conciliaciones; pendientes administrativos próximos a vencer.

Las alertas deberán poder visualizarse en el dashboard y, eventualmente, enviarse por email o notificación interna.

## 13. Búsqueda General "Lupita"

El sistema deberá incluir una búsqueda rápida llamada internamente "Lupita". Lupita deberá permitir buscar: asientos; facturas; clientes; proveedores; proyectos; etapas de proyectos; cuentas contables; pagos; cobros; movimientos bancarios; tarjetas; vencimientos; pendientes administrativos; impuestos; por descripción, número, importe, fecha, CUIT, razón social, proyecto, cuenta o palabra clave.

Desde los resultados, el usuario deberá poder ingresar al registro encontrado para: verlo; editarlo; duplicarlo; anularlo; eliminarlo; según permisos.

## 14. Usuarios, Permisos y Auditoría

### 14.1. Usuarios y permisos

El sistema deberá tener usuarios y permisos. Como en esta primera etapa no se requiere usuario propio para el contador, los roles mínimos podrían ser: administrador; usuario de carga; usuario de solo lectura.

El rol administrador podrá: configurar el sistema; editar asientos automáticos; cerrar períodos; eliminar registros; acceder a todos los reportes; administrar usuarios; modificar configuraciones sensibles.

### 14.2. Auditoría

El sistema deberá tener auditoría. Toda operación importante deberá registrar: usuario; fecha; hora; acción realizada; datos anteriores; datos nuevos.

Esto aplicará especialmente a: asientos contables; facturas; pagos; cobros; impuestos; cierres mensuales; ediciones manuales; eliminaciones; importaciones bancarias; cambios de estado; modificaciones de proyectos; modificaciones de presupuestos; modificaciones de cuentas contables.

## 15. Períodos Contables

El sistema deberá permitir trabajar por períodos contables mensuales. Cada período podrá utilizarse para ordenar, filtrar, visualizar, reportar y controlar la información contable, impositiva y financiera de cada mes.

No necesariamente se deberá cerrar un período contable para poder crear, visualizar, importar o exportar movimientos, asientos, cuentas, facturas o resúmenes. El sistema deberá permitir operar con flexibilidad, incluso cuando existan períodos abiertos, en revisión o cerrados.

El cierre de período deberá funcionar principalmente como herramienta de control para evitar modificaciones accidentales sobre meses ya revisados, conciliados o utilizados para liquidar impuestos. Sin embargo, no deberá bloquear la capacidad general del sistema de consultar, importar, exportar o visualizar información.

Si un período está cerrado y se requiere hacer una modificación sensible, el sistema podrá solicitar permisos especiales, dejar registro en auditoría o permitir ajustes controlados.

## 16. Migración Inicial desde Excel

El sistema deberá contemplar una migración inicial desde el Excel actual. La migración deberá tomar como referencia las hojas existentes de: clientes; proyectos; etapas de proyectos, si existieran; proveedores de servicios; flujo de caja proyectado; flujo de caja detallado mensual; comisiones por ventas; calendario de vencimientos; inversiones en Fondos Fima; presupuesto de pagos; presupuestos estimados de proyectos; libro diario; estado de resultados; plan de cuentas; mayores; estado de situación patrimonial; base de datos de clientes; IVA a pagar; IIBB a pagar; pendientes administrativos.

No necesariamente todo el Excel deberá migrarse como dato definitivo, pero sí deberá utilizarse como fuente para definir estructuras, reglas, reportes, automatizaciones, fórmulas y saldos iniciales.

También deberá contemplarse la importación de facturaciones anteriores y resúmenes bancarios históricos para poder cruzar movimientos con la contabilidad y comenzar a operar con datos lo más completos posible.

## 17. Alcance recomendado para la primera etapa

La primera etapa debería incluir: gestión de clientes; gestión de proveedores; gestión de proyectos; gestión de etapas de proyectos; carga de presupuesto estimado por proyecto; comparación entre presupuesto estimado y resultado real; gestión de comisionistas; gestión de tipos de costos personalizables; plan de cuentas; creación manual de asientos contables; carga de asientos con fechas intermedias sin restricción cronológica; generación automática de asientos desde facturas de venta; generación automática de asientos desde facturas de compra; edición manual de asientos automáticos; posibilidad de agregar cuentas adicionales en asientos de venta sin modificar la factura; estados borrador, confirmado y anulado; mayores contables; facturas de venta ya emitidas; facturas de compra recibidas; adjunto opcional de PDF en facturas; importación de facturaciones anteriores en Excel, CSV o PDF; cuentas por cobrar; cuentas por pagar; cobros y pagos parciales; ARS y USD; tipo de cambio configurable; registración en moneda original y conversión a pesos argentinos; cálculo de diferencias de cambio; saldo inicial en cuentas bancarias, cuentas de dinero y tarjetas; importación de resúmenes de Galicia, Mercado Pago y tarjetas; movimientos importados como pendientes de revisar; gestión de tarjetas de crédito; conciliación bancaria inicial; dashboard; estado de resultados; balance de sumas y saldos; control de pagos y cobros; reporte detallado por proyecto; rentabilidad por proyecto; cálculo de IVA; cálculo de IIBB; carga manual de ajustes impositivos; alertas de vencimientos, pagos, impuestos y movimientos pendientes; pantalla de pendientes administrativos; búsqueda general "Lupita"; exportación a Excel y PDF; auditoría básica.

## 18. Alcance recomendado para una segunda etapa

En una segunda etapa podrían quedar: producto multiempresa completo; usuarios externos para contadores o clientes; integración real con ARCA/AFIP para emisión de facturas; conciliación bancaria más automatizada; lectura avanzada de PDFs bancarios; automatización más profunda de tarjetas; reportes comparativos avanzados; estado de situación patrimonial completo; flujo de caja proyectado avanzado; gestión avanzada de inversiones; módulo avanzado de comisiones; análisis de sueldos más completo; notificaciones por email o WhatsApp; panel comercial para convertir el sistema en SaaS.

## 19. Conclusión funcional

El sistema deberá transformar el Excel actual en una plataforma de gestión contable y financiera. No debe limitarse a copiar las hojas existentes, sino convertir esa lógica en un sistema con entidades claras, relaciones entre módulos, validaciones, reportes, automatizaciones, trazabilidad y control.

La prioridad será que Montanari Tech pueda saber, de forma rápida y confiable: cuánto vendió; cuánto cobró; cuánto tiene por cobrar; cuánto debe pagar; qué impuestos tiene pendientes; cuál es su resultado mensual; cómo está su caja; qué compromisos futuros tiene; cuál es la rentabilidad real de cada proyecto; qué movimientos están pendientes de revisión; qué tareas administrativas siguen abiertas; y qué diferencias existen entre lo presupuestado y lo realmente ejecutado.

El sistema deberá permitir comenzar a operar sin tener que reconstruir toda la historia desde el nacimiento de la sociedad, mediante saldos iniciales, importaciones históricas y movimientos pendientes de revisión. A partir de esa base, deberá permitir ordenar la información, automatizar tareas repetitivas, reducir errores manuales y mejorar el control financiero de Montanari Tech.
