# F4.4 — Cobros y pagos

**Paso:** 25 de 55 · **Fase:** F4 — Facturación, cobros y pagos · **Modelo:** Sonnet 5 · **Depende de:** F4.2, F4.3 · **Plantillas:** PL-4, PL-5
**Checkpoint humano:** ✅ Sí — **PENDIENTE de la validación del contador.** Ver §"Caso para validar" más abajo antes de dar este paso por cerrado.

## Qué se hizo

Tercer y último generador automático de F4 (F4.1 §6): **Cobro** y **Pago**, con imputación multi-factura, diferencia de cambio, la regla del residuo (F3.1 §6.3/§6.4) y anticipos con aplicación posterior diferida (F3.1 §6.5).

**Modelo unificado (cobro/pago = imputación + anticipo, sin caso especial):**
- El usuario informa `total` (bruto cobrado/pagado, moneda original) y 0..N líneas de imputación contra facturas. El **remanente no imputado** (`total − Σ imputaciones`) se congela como `montoAnticipo` al confirmar y genera automáticamente la línea de anticipo (`ANTICIPO_CLIENTE`/`ANTICIPO_PROVEEDOR`). Un cobro/pago con cero líneas es 100% anticipo — **no hay branch especial** para "anticipo puro", es el mismo cálculo con `Σ imputaciones = 0`.
- **Regla del residuo** (F3.1 §6.3): compartida entre Cobro y Pago vía `common/asiento/CalculoImputacion` — la imputación que lleva el saldo en moneda original a cero cancela el CxC/CxP por el saldo ARS contable remanente exacto (no por `round2(imputado × TC_comprobante)`), y la diferencia se absorbe en la línea de diferencia de cambio. Verificado en vivo con el caso CO-3 de F4.1 (dos cobros parciales, TC distinto): el segundo cobro cancela exactamente el saldo remanente y el saldo de la factura llega a cero en ambas monedas.
- **Anticipos con aplicación diferida** (F3.1 §6.5, CO-5): `AplicacionAnticipoCliente`/`AplicacionAnticipoProveedor` son registros *append-only* — cada aplicación posterior genera su **propio asiento `AJUSTE`**, sin editar jamás el asiento original del cobro/pago que generó el anticipo (verificado en vivo: el asiento original quedó bit-a-bit igual después de aplicar el anticipo).
- **Retenciones sufridas** (solo Cobro, F4.1 §6.1): reutilizan `comprobante_tributo` (`ComprobanteTipo.COBRO`), restringidas a `RETENCION_GANANCIAS`/`RETENCION_IVA` (los únicos con cuenta mapeada). Reducen lo que efectivamente ingresa a fondos, no lo que cancela el CxC.
- **Sin retenciones en Pago**: Montanari no es agente de retención (checkpoint F4.1 #3).

**Infraestructura nueva (V22):**
- `CuentaBancaria.cuentaContable` (FK **obligatoria**, no opcional como CxC/CxP): F3.1 §2.3 ya daba por existente esta cuenta contable espejo 1:1, pero F2.4 nunca la agregó. Se agregó la columna, se backfillearon las 3 cuentas bancarias sembradas contra sus cuentas homónimas del plan de cuentas (Banco Galicia CC→1.1.2001, Banco Galicia USD→1.1.2002, Mercado Pago→1.1.2003) y **recién después** se puso `NOT NULL` — a diferencia de CxC/CxP (opcionales, con fallback a mapeo por defecto), cada cuenta bancaria/caja es su propia cuenta contable dedicada, no tiene sentido un "banco genérico" de respaldo.
- `mapeo_cuenta` seed para `DIF_CAMBIO_GANADA`/`DIF_CAMBIO_PERDIDA`/`ANTICIPO_CLIENTE`/`ANTICIPO_PROVEEDOR` (las 4 cuentas que F4.2 había creado en V20 "para consumo futuro" — **bug propio encontrado y corregido durante la verificación en vivo**, ver más abajo).
- `cobro`/`cobro_imputacion`/`aplicacion_anticipo_cliente` y `pago`/`pago_imputacion`/`aplicacion_anticipo_proveedor`.
- `GET /cobros/saldo-venta/{facturaVentaId}` y `GET /pagos/saldo-compra/{facturaCompraId}`: saldo pendiente (moneda original + ARS) calculado en el momento a partir de las imputaciones/aplicaciones confirmadas — no es un campo persistido, evita problemas de invalidación de caché.

## Bugs encontrados y corregidos durante la verificación en vivo

1. **`mapeo_cuenta` sin seed para los 4 conceptos de F4.2/V20.** Al confirmar el primer cobro con diferencia de cambio, `422 MAPEO_CUENTA_FALTANTE` para `DIF_CAMBIO_GANADA`. Las 4 cuentas (6.4005/6.4006/2.1.2018/1.1.2013) se habían creado en V20 pero nunca se sembró su fila de `mapeo_cuenta` (a diferencia de `IVA_DEBITO_FISCAL` que sí se sembró en el mismo V20). Corregido agregando el seed a V22 — son conceptos globales fijos sin discriminador, mismo criterio que `IVA_CREDITO_FISCAL` en V21.
2. **`MONTO_ARS_INCONSISTENTE` en la línea de CxC/CxP y en la línea de diferencia de cambio — bug real de diseño, no solo de seed.** `AsientoService` valida que toda línea en moneda extranjera cumpla `importeOriginal × tipoCambio == monto` exacto. Dos líneas del generador rompían esa invariante:
   - La línea de **diferencia de cambio** se construyó inicialmente en la moneda del cobro/pago (ej. USD) con el TC del cobro — pero la diferencia de cambio es conceptualmente un importe **en pesos** (no "algo en USD convertido"), así que `importeOriginal × TC` nunca iba a coincidir con el monto ARS real. Corregido: la línea de diferencia de cambio siempre va en ARS (`tipoCambio=1`, sin `fuenteTc`) — con eso, `AsientoService` ni siquiera aplica el chequeo (es la misma regla de F3.1 §3.3 para líneas en moneda de libro).
   - La línea de **CxC/CxP** usaba el TC real de la factura — correcto en la imputación normal (el monto cancelado es un producto exacto), pero **no** en la imputación que cierra el saldo por la regla del residuo (ese monto es `total_ars_factura − Σ cancelado_previo`, no necesariamente `imputado × TC_factura`). Corregido: la línea de CxC/CxP usa un TC "efectivo" derivado (`montoCancelado ÷ montoImputado`, redondeado a 6 decimales) que por construcción reproduce exacto el monto ya calculado por la regla del residuo — el TC real de la factura se sigue mostrando en las imputaciones normales (donde ambos valores coinciden) y solo diverge (en una fracción de centavo, invisible en la práctica) en la imputación de cierre.
   - Ambos bugs solo se manifestaban cuando efectivamente había diferencia de cambio o cierre de saldo por residuo — **exactamente el escenario que este paso pide verificar con el contador** — así que se encontraron durante la primera corrida del caso de checkpoint, no antes.

## Caso para validar — CHECKPOINT PENDIENTE

El contador debe confirmar que este asiento generado automáticamente por el sistema es correcto:

> Factura de venta en USD 1.000,00 a un cliente (Valvecchia Gerardo), emitida el 15/06/2026 con TC 1.500,00. El 20/06/2026 se cobra el total en USD a través de Banco Galicia USD, con TC 1.550,00 (subió).
>
> **Asiento generado automáticamente al confirmar el cobro:**
> - Debe Banco Galicia USD — 1.550.000,00
> - Haber Deudores por servicios prestados - Valvecchia Gerardo — 1.500.000,00
> - Haber Diferencia de cambio ganada — 50.000,00

Esto reproduce exactamente el caso CO-2 de la especificación F4.1 (que a su vez es CP-08 de F3.1). También se verificó en vivo el caso de dos cobros parciales con TC distinto (CO-3, con cierre de saldo por la regla del residuo) y el caso de retención de Ganancias sufrida (CO-4) — ambos con el mismo resultado exacto que la especificación. **No se debe considerar F4.4 terminado hasta que el contador confirme el caso de arriba** (o pida ajustes).

## Verificación realizada

- **Compilación** dentro de contenedor `maven:3.9-eclipse-temurin-21`: limpia.
- **19 tests nuevos**: `CobroAsientoGeneratorTest` (CO-1 sin diferencia, CO-2 ganancia, CO-3 dos parciales con residuo, CO-4 retención de Ganancias, CO-5 anticipo puro + aplicación posterior con diferencia perdida, validaciones de moneda/saldo/tributo no aplicable), `PagoAsientoGeneratorTest` (PA-1 pérdida, PA-2 ARS sin diferencia, PA-3 ganancia, anticipo puro, validación de saldo), `CobroServiceTest`/`PagoServiceTest` (ciclo de vida, anticipo, guarda contra anular con aplicaciones vigentes). Además se actualizaron `CuentaBancariaServiceTest` por la nueva FK obligatoria.
- **244/245 tests** en la suite completa (el único que falla es el Testcontainers/Docker Desktop local ya conocido, ver [[build-env-jdk21-testcontainers]]).
- **Verificación end-to-end contra el servidor real** (migraciones V1→V22 desde volumen limpio, puertos temporales 3308/8083 por los mismos motivos que F4.2/F4.3):
  - Backfill de `cuenta_bancaria.cuenta_contable_id`: las 3 cuentas sembradas quedaron correctamente linkeadas.
  - **CO-2** (el caso de checkpoint): asiento exacto, balanceado.
  - **CO-3**: dos cobros parciales (USD 400 @1.520 y USD 600 @1.490 contra una factura USD 1.000 @1.500) — el segundo cobro cancela exactamente el saldo remanente (900.000,00, igual al cálculo directo en este caso particular) y el saldo de la factura llega a 0,00 en ambas monedas (`GET /cobros/saldo-venta/{id}`).
  - **CO-4**: retención de Ganancias sufrida — Debe Banco 119.000 / Debe Anticipo Imp. a las Ganancias 2.000 / Haber CxC 121.000.
  - **CO-5**: anticipo puro (cobro sin facturas) confirmado, luego aplicado contra una factura con TC distinto — asiento `AJUSTE` con la diferencia de cambio correcta; el asiento original del anticipo quedó intacto.
  - **PA-1**: pago USD con TC mayor — pérdida, exacto a la especificación.
  - Anulación: anular un cobro sin aplicaciones de anticipo cascadea correctamente a su asiento; anular uno **con** aplicaciones de anticipo vigentes lo rechaza con `COBRO_CON_APLICACIONES_DE_ANTICIPO` (guarda nueva, para no dejar asientos `AJUSTE` húérfanos apuntando a un cobro anulado).
  - Permisos: usuario `LECTURA` real recibe 403 al crear cobro o pago, 200 en las lecturas.
- Frontend: `tsc -b` y `oxlint` limpios sobre `cobros-page.tsx`, `pagos-page.tsx`, `proveedores-page`/`cuentas-bancarias-page` actualizados y el wiring de router/nav (solo los 2 warnings preexistentes en componentes UI compartidos).
