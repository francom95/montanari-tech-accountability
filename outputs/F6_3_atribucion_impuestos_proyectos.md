# F6.3 — Atribución de impuestos liquidados a proyectos

**Paso:** 34 de 55 · **Fase:** F6 — Impuestos · **Modelo asignado:** Sonnet 5 (ejecutado con Opus 4.8 por elección del usuario) · **Depende de:** F6.1, F6.2
**Checkpoint humano:** No.

## Qué se hizo

Atribuir el impuesto liquidado (IVA o IIBB de una liquidación **confirmada**) a los proyectos, para alimentar el reporte de rentabilidad de F7.4. La atribución se guarda por liquidación y se puede rehacer con distintos criterios.

### Referencia polimórfica a la liquidación

Una atribución apunta a la liquidación por `liquidacionTipo` (IVA/IIBB) + `liquidacionId`, **sin FK real**, mismo patrón que `ComprobanteTributo` (F4.1). Así el mismo mecanismo sirve para IVA y para IIBB sin tocar esas entidades ni duplicar código. Un único registro por liquidación (unique constraint), reescribible.

### El monto a repartir

Es el **saldo a pagar** de la liquidación (para IVA `saldoAPagar`, para IIBB `saldoAPagarTotal`): el costo fiscal efectivo del período. Si la liquidación da saldo a favor (nada a pagar), no hay nada que atribuir y la UI lo informa. Siempre en ARS (los impuestos se liquidan en pesos), aunque se guardan moneda + TC + importe por la regla multimoneda.

### Los cuatro criterios de prorrateo

- **DIRECTO**: 100% a un proyecto.
- **FACTURACION**: proporcional a las ventas netas de cada proyecto en el período (facturas de venta confirmadas; las notas de crédito restan). Las ventas sin proyecto se avisan y quedan afuera.
- **MARGEN**: proporcional al margen (ventas − compras) de cada proyecto en el período. Solo proyectos con margen positivo; si ninguno tiene margen positivo, se avisa.
- **PORCENTAJE_MANUAL**: porcentajes cargados a mano, que deben sumar 100 (validado).

El criterio por defecto es un **parámetro del sistema** (`ConfiguracionAtribucion`, una fila, editable solo por admin), con **override por liquidación** (cada atribución guarda su propio criterio).

### La regla del residuo (lo central del paso)

`ProrrateoCalculator` es una función pura que reparte un monto según pesos por proyecto: cada línea salvo la última se redondea a centavos (`round2(total × peso / Σpesos)`), y **la última línea toma el remanente exacto** (`total − acumulado`), absorbiendo el residuo de todos los redondeos previos. Así la suma de las líneas es **siempre** exactamente el total, sin error de centavos — mismo principio que `CalculoImputacion` (F4.4). Se aisló en una clase sin dependencias para testear el reparto por separado.

### Persistencia para F7.4

`AtribucionImpuestoLinea` (proyecto, porcentaje, monto) queda persistida y consultable por `findByAnioAndMes`, para que el reporte de rentabilidad por proyecto la consuma sin recalcular.

### Frontend

Componente reutilizable `AtribucionProyectos` (tipo + liquidacionId) embebido en la sección de detalle de **ambas** liquidaciones (IVA e IIBB), visible solo cuando están **confirmadas**: elegir criterio, previsualizar la distribución, y guardar.

## Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **423/424 en verde** (el único fallo es el de Testcontainers/Docker Desktop local). **12 nuevos**: `ProrrateoCalculatorTest` (5, incluye 3 proyectos que suman exacto y el residuo en la última línea), `AtribucionImpuestoServiceTest` (7: facturación, directo, margen, porcentaje manual que suma/no suma 100, borrador rechazado, ventas sin proyecto).
- Frontend: `tsc -b` y `oxlint` limpios.

### Bug real detectado y corregido

Al levantar el stack, el backend entró en bucle de reinicio: `ProyectoRepository.buscar(...)` había quedado **sin su `@Query`** (la anotación se pegó por error al método nuevo `findByActivoTrueOrderByNombreAsc`), y Spring Data intentaba derivar la consulta del nombre `buscar` → `No property 'buscar' found for type 'Proyecto'`, cancelando la creación del contexto. Quedó **enmascarado en la suite** porque el único test que arranca el contexto completo es el de Testcontainers, que ya falla en Docker Desktop local. Corregido devolviendo el `@Query` a `buscar` (el `findBy...` derivado no lo necesita). Sin esta corrección la app no arranca.

### E2E contra MySQL 8 real (docker-compose)

Escenario: ventas de marzo por proyecto (Alfa 1.000.000, Beta 600.000, Gamma 400.000; débito IVA 21% = 420.000, sin crédito → saldo a pagar 420.000), atribución del saldo por distintos criterios.

| Paso | Resultado | ✔ |
|------|-----------|---|
| Config default del sistema | `criterioPorDefecto = FACTURACION` | ✔ |
| Liquidación IVA marzo confirmada | estado `CONFIRMADO`, saldo a pagar `420000.00` | ✔ |
| Atribución **FACTURACION** | Alfa 50% = 210.000 · Beta 30% = 126.000 · Gamma 20% = 84.000 | ✔ |
| **Suma de las líneas** | `420000.00` = exactamente el monto total (regla del residuo) | ✔ |
| Re-atribución **DIRECTO** a Beta (override) | Beta 100% = 420.000, reescribe la anterior | ✔ |
| Previsualizar **PORCENTAJE_MANUAL** 50+30 | rechazado: `ATRIBUCION_MANUAL_NO_SUMA_100` (suman 80,00) | ✔ |

La suma de las atribuciones coincide **al centavo** con el saldo a pagar de la liquidación, confirmando la regla del residuo en la última línea.
