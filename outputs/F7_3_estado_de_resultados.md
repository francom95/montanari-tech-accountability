# F7.3 — Estado de resultados

**Paso:** 37 de 55 · **Fase:** F7 — Reportes y dashboard · **Modelo:** Sonnet 5 · **Depende de:** F7.2
**Checkpoint humano:** No.

## Qué se hizo

Estado de resultados con apertura fija de 9 líneas (ingresos por ventas, otros ingresos por ventas, costos de prestación de servicios, gastos de comercialización/administración/financieros, impuestos, otros ingresos, otros egresos) y 3 subtotales (bruto, operativo, final), en 4 vistas (mes, año, acumulado, por proyecto), con comparativo mes vs mes anterior, mapeo rubro→línea configurable y drill-down a las cuentas.

### El gap real del plan de cuentas (decisión del usuario)

El seed real (V17) tiene solo 4 rubros de resultado, y uno de ellos ("Gastos Operativos") junta sueldos, honorarios, intereses, impuesto a las ganancias e IIBB en un solo rubro — mientras el ER de 9 líneas separa eso en 4 (comercialización/administración/financieros/impuestos). Se le preguntó al usuario cómo seguir: **eligió no tocar el seed** — el mapeo se construye tal como está pedido (configurable), "Gastos Operativos" mapea entero a `GASTOS_DE_ADMINISTRACION` como default aceptado, y el admin puede reclasificar cuentas a rubros más finos con el CRUD ya existente sin que este paso reescriba datos de referencia que F3.3 ya decidió.

### La clave del mapeo es (rubro, naturaleza), no solo rubro

Un segundo problema de granularidad, no cubierto por la pregunta anterior: el rubro "Otros Ingresos y Egresos" mezcla cuentas RP (comisiones ganadas) y RN (comisiones bancarias) — mapear solo por rubro colapsaría "Otros ingresos" y "Otros egresos" en la misma línea. Se resolvió agregando `naturaleza` (RP/RN) a la clave del mapeo (`MapeoRubroLineaEstadoResultados`, único por tenant+rubro+naturaleza) — separa correctamente ambos casos sin tocar el plan de cuentas, y de paso también da más flexibilidad a futuro si otro rubro mixto aparece.

### Motor de cálculo: reusa la query agregada de F7.2, agrupa distinto

Misma `AsientoLineaRepository.sumarDebeHaberPorCuenta` de F7.2 (ahora con un parámetro `proyectoId` agregado para la vista por proyecto), pero en vez de hacer roll-up por jerarquía de cuenta, cada cuenta se bucketiza por `(rubro, naturaleza) → línea`. El monto de cada cuenta se signa según su naturaleza real (RP → haber−debe, RN → debe−haber) para que ingresos y gastos salgan siempre positivos y los subtotales sean sumas/restas literales. Una cuenta cuyo rubro no tiene mapeo para esa naturaleza cae en un bucket **"sin mapear"**, visible en el reporte (pantalla y export) — nunca se descarta en silencio.

### Las 4 vistas

- **Por mes**: `YearMonth.of(anio,mes)`, con comparativo contra el mes anterior (variación absoluta y %; % es `null` si el mes anterior dio exactamente cero, para evitar división por cero).
- **Por año**: rango 1/enero a 31/diciembre, sin comparativo (no aplica).
- **Acumulado**: 1/enero hasta fin del mes pedido.
- **Por proyecto**: una fila por proyecto activo con movimiento en el mes (los que no tuvieron ningún movimiento se omiten, no se listan en cero) + un bucket **"sin proyecto"** con una query nueva (`sumarDebeHaberPorCuentaSinProyecto`, con `l.proyecto IS NULL` explícito — un `proyectoId = null` en la query normal significa "sin filtrar", no "sin proyecto").

### Drill-down

Cada línea trae la lista de cuentas que la componen con su monto. En el frontend, cada cuenta linkea al Mayor (F3.6) de esa cuenta.

## Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **454/455 en verde** (el único fallo es el de Testcontainers/Docker Desktop local, esperado). **15 nuevos**: `MapeoRubroLineaEstadoResultadosServiceTest` (6: rubro inexistente, naturaleza fuera de RP/RN, crear/editar/eliminar con auditoría, duplicado), `EstadoResultadosServiceTest` (9: subtotales con las 9 líneas, cuenta sin rubro a "sin mapear", comparativo mes vs anterior con variación %, variación % nula con mes anterior en cero, vista por año, vista acumulada, vista por proyecto con bucket sin proyecto).
- Frontend: `tsc -b` y `oxlint` limpios.
- **E2E contra MySQL 8 real** (docker-compose): mapeos sembrados (V32) confirmados vía API; dos asientos confirmados (venta mayo 200.000, venta abril 100.000); vista por mes con comparativo (variación +100.000, +100%); vista por año y acumulada ambas en 300.000 (mayo+abril); drill-down de INGRESOS_POR_VENTAS mostrando la cuenta 4.1.2001; export Excel y PDF con contenido real.
