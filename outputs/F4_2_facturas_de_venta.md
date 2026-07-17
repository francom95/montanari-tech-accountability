# F4.2 — Facturas de venta

**Paso:** 23 de 55 · **Fase:** F4 — Facturación, cobros y pagos · **Modelo:** Sonnet 5 · **Depende de:** F4.1 · **Plantillas:** PL-4, PL-5
**Checkpoint humano:** No.

## Qué se hizo

Sobre la especificación de F4.1, se implementó el primer generador automático de asientos (ADR-07: `AsientoService` sigue siendo el único punto de escritura) y el ciclo de vida completo de facturas de venta.

**Infraestructura compartida (V20):**
- 4 cuentas nuevas resueltas desde el checkpoint de F4.1: 6.4005 Diferencia de cambio ganada, 6.4006 Diferencia de cambio perdida (rama 6), 2.1.2018 Anticipos de clientes (pasivo), 1.1.2013 Anticipos a proveedores (activo).
- **`mapeo_cuenta`**: tabla concepto→cuenta con discriminador opcional (`concepto`, `discriminadorTipo`, `discriminadorValor`) y fallback a fila por defecto — implementa la indirección de F4.1 para que los generadores nunca hardcodeen códigos de cuenta. Seed: `IVA_DEBITO_FISCAL` (default), `INGRESO_VENTA` con discriminador `TIPO_INGRESO` (`VENTA`→4.1.2001, `OTRA_VENTA`→4.1.2002). Deliberadamente **sin** fila por defecto para `CREDITO_POR_VENTA` (cada empresa define si usa cuenta por cliente o una genérica).
- **`Cliente.cuentaCxcId`** (checkpoint F4.1: mantener cuentas por cliente): override opcional; si no está seteada, `ResolutorCuentas` cae al mapeo `CREDITO_POR_VENTA`.
- **`adjunto`** (tabla genérica por entidad/id) y **`comprobante_tributo`** (percepciones/retenciones futuras, F4.3+): infraestructura compartida, sin lógica de negocio propia todavía.
- **`AsientoService.registrarAutomatico(AsientoGenerado)`**: nuevo método, único punto de entrada para los generadores de F4.x. Corrige un bug latente del molde PL-4 original (F1.8): el número de asiento se pide *después* de validar el balance, nunca antes, para no dejar huecos de numeración si la validación falla. Se eliminó `GeneradorAsientoDePrueba` (demo obsoleta que reproducía ese bug).

**Dominio `FacturaVenta`:**
- Entidad con líneas (`FacturaVentaLinea`: importe neto, alícuota IVA, tipo GRAVADO/NO_GRAVADO/EXENTO, tipo de ingreso VENTA/OTRA_VENTA), moneda/tipo de cambio propios, estado (`EstadoDocumento` reusado de F3.x).
- `FacturaVentaAsientoGenerator`: arma 2-4 líneas (ingreso por línea + IVA si corresponde + CxC), invierte débito/crédito automáticamente si `tipoComprobante` empieza con `NOTA_CREDITO_` (ADR-13).
- `FacturaVentaService`: `crearBorrador`/`editarBorrador`/`eliminarBorrador`/`confirmar`/`anular`, validando alícuotas de IVA contra el set permitido (0/2.5/5/10.5/21/27) y recalculando totales desde las líneas.
- CRUD de `mapeo_cuenta` con UI admin-only para lectura/escritura de las reglas de imputación.
- Frontend: `facturas-venta-page.tsx` (formulario cabecera+líneas al estilo `asientos-page.tsx`, con selector de cuenta override por línea) y `mapeo-cuenta-page.tsx`, con adjuntos (subida/descarga autenticada) integrados en la factura.

## Decisiones tomadas (sin checkpoint, documentadas para trazabilidad)

1. **Cada línea materializa su propio importe a ARS de forma independiente**, en vez de calcular la línea de CxC como suma de las demás. Evita un conflicto con el chequeo `MONTO_ARS_INCONSISTENTE` de `AsientoService` (que exige que `importeOriginal × tipoCambio` redondeado coincida con el monto de cada línea en moneda extranjera). En el caso extremo donde el redondeo independiente de cada línea no sume exactamente el total, `ValidadorBalanceAsiento` rechaza el asiento con `ASIENTO_NO_BALANCEA` — no se fuerza un ajuste silencioso.
2. **`tipoIngreso` como campo nuevo en `FacturaVentaLinea`** (no estaba en el diccionario literal de F1.1): es el discriminador que hace resoluble el mapeo `INGRESO_VENTA`/`TIPO_INGRESO` ya definido en la especificación de F4.1 — sin este campo esa distinción sería imposible de implementar.
3. **Bug real encontrado y corregido durante la verificación** (ver más abajo): `AsientoService.anular` es un único método usado tanto por el endpoint directo `/asientos/{id}/anular` como (hasta ahora) por los servicios de documento. Su regla "rechazar orígenes que no sean MANUAL/AJUSTE/APERTURA/IMPORTACION" es correcta para uso directo, pero bloqueaba también la anulación *legítima* que dispara `FacturaVentaService.anular` sobre su propio asiento vinculado. Se separó en `anular` (uso directo, con el chequeo) y `anularPorDocumento` (uso interno de servicios de documento F4.x, sin el chequeo — ellos SON "el comprobante" al que remite el mensaje de error). `FacturaVentaService` ahora llama a `anularPorDocumento`.

## Verificación realizada

- **Compilación** dentro de contenedor `maven:3.9-eclipse-temurin-21` (host tiene JDK 24, rompe Lombok): limpia.
- **20 tests nuevos**: `FacturaVentaAsientoGeneratorTest` (FV-1 ARS con IVA 21% balancea exacto, FV-2 USD sin IVA con fallback de CxC cuando el cliente no tiene cuenta propia, FV-3 OTRA_VENTA resuelve 4.1.2002, inversión de signo en nota de crédito, propagación de `MAPEO_CUENTA_FALTANTE`, `FACTURA_SIN_IMPORTE`), `FacturaVentaServiceTest` (ciclo de vida completo, transición inválida, anulación cascada), `ResolutorCuentasTest` (fila específica > fila por defecto > error, cuenta no imputable/inactiva).
- **191/192 tests** en la suite completa (el único que falla es el Testcontainers/Docker Desktop local ya conocido, ver [[build-env-jdk21-testcontainers]]).
- **Verificación end-to-end contra el servidor real** (`docker compose up`, cadena completa V1→V20 desde volumen limpio):
  - Factura ARS con IVA 21% (cliente con cuenta CxC propia `1.1.2004.01`): asiento de 3 líneas, debe=haber=121.000,00, resuelve 4.1.2001 e IVA Débito Fiscal 2.1.2008 correctamente.
  - Nota de crédito USD sin IVA, tipo de ingreso OTRA_VENTA, cliente **sin** cuenta CxC propia: resuelve 4.1.2002 y cae al mapeo por defecto de `CREDITO_POR_VENTA`; débito/crédito invertidos respecto de una factura normal; totalArs = 1.000 × 1.500 = 1.500.000 correcto.
  - **Acá se encontró y corrigió el bug de anulación cascada** (punto 3): el primer intento de anular la nota de crédito devolvía `ANULACION_VIA_DOCUMENTO` 422 pese a venir del propio comprobante. Tras el fix, `anular` sobre la factura pone en ANULADO tanto la factura como su asiento vinculado; se confirmó además que `/asientos/{id}/anular` directo sigue rechazando ese mismo asiento (el chequeo de uso directo sigue vigente).
  - Adjuntos: subida multipart (`POST /adjuntos?entidadTipo=FacturaVenta&entidadId=…`), listado y descarga devuelven el archivo con nombre/mime/tamaño correctos.
  - Permisos: usuario `LECTURA` real recibe 403 al intentar crear `mapeo_cuenta`, crear factura o subir adjunto, y 200 en las lecturas equivalentes — conforme a la matriz de F3.1 §4.6.
- Frontend: `tsc --noEmit` y `oxlint` limpios sobre `facturas-venta-page.tsx`, `mapeo-cuenta-page.tsx` y el wiring de router/nav; servidor de desarrollo levanta sin errores de consola.
