# F7.4 — Reporte detallado por proyecto (rentabilidad)

**Paso:** 38 de 55 · **Fase:** F7 — Reportes y dashboard · **Modelo asignado:** Sonnet 5 · **Depende de:** F7.3, F6.3 (y consume F2.6, F2.7, F4.4/F4.5)
**Checkpoint humano:** Sí — validar con un proyecto real cerrado que el margen coincida con el Excel. Ver "Checkpoint pendiente" al final.

## Qué se hizo

El reporte clave del sistema: por proyecto, agrega — sin recalcular — el presupuesto interno (F2.6, USD), la ejecución real (facturación/cobros/pagos F4.4-F4.5), las comisiones (F2.7) y los impuestos atribuidos (F6.3), y muestra el margen estimado vs. real y la rentabilidad final.

Al diseñar la comparación surgieron tres preguntas reales que el plan no resolvía, resueltas por el usuario en esta sesión — el alcance terminó siendo bastante más grande que "un reporte":

### Conversión USD→ARS del presupuesto (nueva, específica de este paso)

El presupuesto (F2.6) es un monto único en USD sin fecha propia; lo real (facturas) ya tiene TC histórico persistido (`FacturaVenta.tipoCambio`/`totalArs`). Se empareja por **orden** cada `ProyectoCuota` con la N-ésima `FacturaVenta` confirmada del proyecto (por fecha ascendente), se divide el presupuesto en `cantidadPagosPactados` partes iguales, y cada parte se convierte con el TC de **esa** factura puntual. Las cuotas sin factura real todavía no se convierten — quedan fuera de la comparación, marcadas en `advertencias`, en vez de asumir un TC.

### Criterio de TC por defecto (infraestructura transversal, `ConfiguracionTipoCambio`)

`TipoCambioRepository.findFirstByMonedaIdAndFechaAndActivoTrueOrderByIdAsc` (único caller: `AsientoService.resolverTipoCambioAutomatico`) ignoraba el criterio (BNA venta/compra, oficial...) por completo — gap documentado en su propio Javadoc. Se agregó un parámetro de sistema (`criterioPorDefecto`, nullable) editable por admin desde la pantalla de Tipos de Cambio: si está configurado, se prueba primero la cotización de ese criterio; si no hay coincidencia (o no está configurado), cae al comportamiento de siempre. `criterioPorDefecto = NULL` (default del seed) preserva 100% el comportamiento anterior — ningún test existente cambió.

### Mora en cobros (nueva línea contable real, no solo informativa)

No había ningún antecedente — ni campo, ni concepto, ni cuenta. El usuario confirmó que debía impactar la contabilidad real: `CobroImputacion.recargoMoraOriginal` (nullable, misma moneda que el monto imputado), nuevo `ConceptoContable.INTERES_POR_MORA_GANADO` mapeado a la cuenta `6.4002 "Intereses Ganados"` (sembrada en F3.3, nunca usada hasta ahora — no hizo falta cuenta nueva). `CobroAsientoGenerator` agrega, de forma aditiva (nunca se activa si el campo es `null`/0), una línea Haber al recargo con el TC del cobro, y suma ese monto a `sumaImputadoOriginal` para que no se cuele como anticipo. Nueva `ConfiguracionCobranza` (días de gracia + tasa diaria, editable por admin) para sugerir el importe; el frontend muestra los días de atraso (factura vencida vs. fecha del cobro) y deja el campo editable.

### El agregador (`ReporteRentabilidadProyectoService`)

Reusa tal cual lo que ya existía: `ComisionProyectoRepository.findByProyectoIdAndActivoTrue`, `FacturaVentaRepository`/`FacturaCompraRepository.buscarConfirmadasParaReporte` (con `proyectoId`, resto `null`), `CobroImputacionRepository`/`PagoImputacionRepository.sumarImputadoPorFactura` + las de aplicación de anticipo, `PresupuestoProyectoService.obtener/calcular`. Se agregaron dos queries nuevas: `AtribucionImpuestoLineaRepository.findByProyectoId` (no existía — las líneas solo se alcanzaban vía la colección de `AtribucionImpuesto`) y `EtapaRepository.findByProyectoIdOrderByFechaInicioAsc` (solo había versión paginada). Ingresos/egresos/comisiones-en-ARS/impuestos siempre en ARS; las comisiones en otra moneda se excluyen del margen ARS y se avisan en `advertencias` (no se fuerza una conversión sin TC histórico).

## Verificación realizada

- **Suite completa** en local (JDK 21): **468/469 en verde** (único fallo esperado: Testcontainers/Docker Desktop local). Nuevos: `ReporteRentabilidadProyectoServiceTest` (emparejamiento cuota↔factura, conversión parcial, margen real), casos de recargo por mora en `CobroAsientoGeneratorTest`, casos de criterio de TC en `AsientoServiceTest`.
- Frontend: `tsc -b` y `oxlint` limpios.

### Bug real detectado y corregido durante este paso

El mensaje de advertencia de pagos sin emparejar usaba `emparejados` en vez de `cantidadPagos - emparejados` (decía "2 de 3 pagos sin factura" cuando en realidad 2 SÍ tenían factura y solo 1 no) — encontrado por el test `emparejaCuotasConFacturasPorOrdenYConvierteSoloLaPorcionEmparejada`, corregido antes de cerrar la fase.

### E2E contra MySQL 8 real (docker-compose)

Proyecto Argentina con presupuesto (costo 1000, margen 500 USD → precio final 2482,75 USD), 2 facturas de venta en USD con TC distinto (1000 y 1100), 2 cobros (uno a tiempo, uno con 9 días de atraso y USD 20 de recargo), 1 comisión en ARS.

| Verificación | Resultado | ✔ |
|---|---|---|
| Asiento del cobro con mora | Debe Banco 1.353.000 = Haber CxC 1.331.000 + Haber Interés Ganado 22.000 | ✔ |
| Días de atraso mostrados (backend y UI) | 9 días (2026-03-01 vs. vencimiento 2026-02-20) | ✔ |
| Presupuesto convertido (porción emparejada) | 2.606.890,46 ARS (2 de 2 pagos emparejados) | ✔ |
| Facturado real (misma porción) | 2.541.000,00 ARS | ✔ |
| Diferencia | -65.890,46 ARS | ✔ |
| Margen real | 2.540.800,00 ARS (cobrado − pagado − comisiones − impuestos atribuidos) | ✔ |
| Export Excel/PDF | HTTP 200, streaming no vacío | ✔ |
| Config TC por defecto y Config Cobranza | GET/PUT funcionando, reflejado en la UI (`tipos-cambio-page`, `cobros-page`) | ✔ |

### Checkpoint pendiente

El plan exige validar con un proyecto real cerrado que el margen coincida con el Excel del contador. Este paso quedó verificado con un escenario propio (E2E arriba) que reconcilia matemáticamente al centavo, pero **no hay todavía un proyecto real cerrado** para contrastar contra el Excel de rentabilidad del contador — pendiente de que el usuario aporte uno, igual que el checkpoint de F2.6 se cerró después con un caso real.
