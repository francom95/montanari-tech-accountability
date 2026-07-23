# F5.4 — Tarjetas de crédito (operatoria completa)

**Paso:** 31 de 55 · **Fase:** F5 — Bancos y conciliación · **Modelo:** Sonnet 5 · **Depende de:** F5.3
**Checkpoint humano:** No.

## Qué se hizo

Ciclo completo del resumen de tarjeta: **importar** (reusa el `ParserTarjeta` de F5.2 sin cambios) → **clasificar** los consumos (manual o masivo por reglas) → **pagar** el resumen (total o parcial) → **conciliar** el pago contra el movimiento bancario correspondiente, reusando el motor de F5.3 sin tocarle una línea.

### Infraestructura nueva

- `TarjetaCredito.cuentaContable` (nullable, sin backfill): la tarjeta ahora tiene su propia cuenta de pasivo ("Tarjeta de Crédito a Pagar", nueva 2.1.2019). Nullable porque no hay forma segura de adivinar qué cuenta le corresponde a una tarjeta ya persistida en datos reales del usuario; se valida a nivel de aplicación (`TARJETA_SIN_CUENTA_CONTABLE`) recién cuando hace falta generar un asiento, no a nivel de esquema.
- `ConsumoTarjeta`: un consumo importado del resumen (fecha, descripción, importe, moneda/TC si es en USD, clasificación opcional a cuenta/proveedor/proyecto/concepto). Mismo patrón de dedup por hash (`hash_importacion`, único por tarjeta) que `MovimientoBancario` en F5.2.
- `ReglaClasificacionConsumo`: motor de reglas **genuinamente nuevo**, a diferencia del `ClasificadorMovimientoBancario` hardcodeado de F5.3 — acá las categorías (proveedores, suscripciones específicas del usuario) son inherentemente propias del negocio, no patrones genéricos de banco argentino. CRUD completo por el usuario: patrón de texto (único) → cuenta contable (+ proveedor/proyecto/concepto opcionales).
- `PagoTarjeta`: documento PL-5 (BORRADOR/CONFIRMADO/ANULADO) que representa el pago del resumen, total o parcial.

### Clasificación de consumos

- Manual: `PATCH /consumos-tarjeta/{id}/clasificar`.
- Masiva: `POST /tarjetas-credito/{id}/consumos-tarjeta/clasificar-masivamente` — recorre los consumos sin clasificar de esa tarjeta, aplica la **primera regla activa** cuyo patrón esté contenido (`contains`) en la descripción, en orden alfabético de patrón (determinístico). No hay ranking ni scoring: la primera que matchea, gana.

### Pago del resumen: reusa el mismo molde PL-4 que Cobro/Pago

`PagoTarjetaAsientoGenerator` (implementa `AsientoGenerator<PagoTarjeta>`) genera un asiento de 2 líneas: **Debe** la cuenta contable de la tarjeta (reduce el pasivo), **Haber** la cuenta contable de la `cuentaBancariaDebito` de la tarjeta — **con `cuentaBancariaId` seteado en esa línea de fondos**, exactamente igual que hacen `Cobro` y `Pago` (F4.4) y `MovimientoBancarioService` (F5.1). Origen `RESUMEN_TARJETA` (enum ya predeclarado desde F3.4 anticipando este paso).

**Un pago parcial (pago mínimo) no tiene lógica especial**: simplemente se registra el importe pagado, y el resto queda reflejado en el saldo de la tarjeta como pendiente — no existe un concepto separado de "saldo pendiente del resumen", es directamente el `saldoActual` de la tarjeta.

### Por qué la conciliación de F5.3 no necesitó ningún cambio

La clave arquitectónica de F5.3 fue que `AsientoLinea.cuentaBancaria` identifica *cualquier* asiento que haya movido una cuenta bancaria, sin importar qué documento lo generó. Como `PagoTarjetaAsientoGenerator` setea ese mismo campo en su línea de fondos, el pago de un resumen de tarjeta queda **automáticamente disponible** como candidato de conciliación (`AsientoLineaRepository.buscarCandidatosConciliacion`) apenas se confirma — verificado end-to-end (ver abajo), sin escribir ni una línea nueva en el paquete `bancos.conciliacion`.

### RecalculoSaldoService: primera implementación real (para tarjetas)

Desde F5.1, `RecalculoSaldoService.recalcular` era un stub que siempre devolvía `saldoInicial` sin importar los movimientos — un gap detectado en F5.3 y dejado deliberadamente sin resolver para `CuentaBancaria` (pendiente para F8.3). En F5.4 se agregó la primera rama real: para `TarjetaCredito`, `saldoActual = saldoInicial + Σ ConsumoTarjeta.importeArs + Σ PagoTarjeta.importeArs confirmados`, ambos con fecha posterior a `fechaSaldoInicial`. La rama de `CuentaBancaria` sigue intacta (stub).

Los consumos ya vienen **negativos** (egreso) por convención de `ParserTarjeta` (F5.2: "un importe sin signo explícito es un consumo/impuesto, se negativiza"). Los pagos se cargan **positivos** (el usuario ingresa el importe pagado como un monto positivo). Por lo tanto un pago debe **sumarse** al saldo para reducir la deuda (acercarla a cero) — no restarse. *(Ver bug real corregido, abajo.)*

### Frontend

- `tarjeta-credito-detalle-page.tsx`: importar resumen (previsualizar → confirmar), tabla de consumos con clasificación inline y botón "Clasificar masivamente", registrar/confirmar/anular pago, saldo actual.
- `reglas-clasificacion-consumo-page.tsx`: CRUD simple de reglas.
- `tarjetas-credito-page.tsx`: se agregó el campo obligatorio `cuentaContableId` al alta/edición y un link "Ver detalle" hacia la nueva página.

## Bug real encontrado y corregido en este paso

`RecalculoSaldoService.recalcularTarjeta` restaba el importe de los pagos confirmados en vez de sumarlo. Con la convención real de signos (consumos negativos, pagos positivos), restar un pago **aumentaba** la deuda en vez de reducirla. Detectado recién en la verificación end-to-end contra datos reales (25 consumos importados del PDF real de Visa, saldo `-610921.25`; tras confirmar un pago parcial de `$150.000` el saldo pasó a `-760921.25` en vez de `-460921.25`). Corregido cambiando `.subtract(...)` por `.add(...)` en `RecalculoSaldoService.java`, y se reescribió el test unitario correspondiente (`RecalculoSaldoServiceTest`) con signos realistas para que hubiera fallado si el bug seguía presente.

## Verificación realizada

- **Compilación y suite completa** en contenedor `maven:3.9-eclipse-temurin-21`: 19 tests nuevos (`PagoTarjetaAsientoGeneratorTest`, `ConsumoTarjetaServiceTest`, `ImportacionConsumoTarjetaServiceTest`, `PagoTarjetaServiceTest`, `ReglaClasificacionConsumoServiceTest`) + 3 tests de `RecalculoSaldoServiceTest` reescritos tras el fix, todos verdes; suite completa 344/345 (el único que falla es el Testcontainers/Docker Desktop local ya conocido, no relacionado).
- **Verificación end-to-end vía docker-compose** (migraciones V1→V26 desde volumen limpio), contra el PDF real de resumen Visa (F5.2):
  1. Alta de tarjeta con `cuentaContableId` → cuenta 2.1.2019.
  2. Importación del PDF real: 25 consumos, deduplicación por hash verificada (reimportar el mismo archivo no duplica).
  3. Clasificación masiva vía regla "DONWEB" → 8 consumos clasificados automáticamente (coincide exactamente con las 8 filas reales de esa suscripción); clasificación manual de 1 consumo adicional ("COMISIÓN MANT DE CTA." → 6.4003).
  4. Saldo de la tarjeta tras la importación: `-610921.25` (correcto, verificado contra la suma real de las 25 filas del PDF).
  5. Pago parcial (pago mínimo) de `$150.000`, confirmado → asiento generado: Debe 2.1.2019 / Haber 1.1.2001 (cuenta contable de "Banco Galicia CC"), con `cuentaBancariaId=1` en la línea de fondos, origen `RESUMEN_TARJETA`.
  6. Saldo de la tarjeta tras el pago (con el fix aplicado): `-460921.25` — confirma que un pago parcial reduce la deuda y deja el resto como saldo pendiente, sin lógica especial.
  7. Movimiento bancario manual creado en la misma cuenta (Banco Galicia CC), mismo importe y fecha → `GET /conciliacion/resumen` devuelve `matchSugerido` apuntando exactamente al asiento del pago de tarjeta (`origenTipo: "PagoTarjeta"`), con `saldoBanco == saldoSistema` (diferencia `0.00`) — **prueba end-to-end de que F5.3 no necesitó ningún cambio de código** para conciliar pagos de tarjeta.
- Frontend: `tsc -b` y `oxlint` limpios.
- `docker compose down -v` y reversión de puertos (3308→3307, 8083→8081) confirmada sin diff en `docker-compose.yml` antes de commitear.

## Gaps heredados, sin cambios en este paso

- `RecalculoSaldoService` para `CuentaBancaria` sigue siendo el stub dejado por F5.1/F5.3 (pendiente de F8.3).
- El límite de `fechaSaldoInicial` (movimientos anteriores a esa fecha no se cuentan) aplica igual a `TarjetaCredito` que a `CuentaBancaria` — mismo comportamiento ya documentado y aceptado en F5.3.
