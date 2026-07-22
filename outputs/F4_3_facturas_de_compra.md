# F4.3 — Facturas de compra

**Paso:** 24 de 55 · **Fase:** F4 — Facturación, cobros y pagos · **Modelo:** Sonnet 5 · **Depende de:** F4.2 · **Plantillas:** PL-4, PL-5
**Checkpoint humano:** No.

## Qué se hizo

Segundo generador automático de asiento (F4.1 §5), simétrico a F4.2 (facturas de venta) pero con dos piezas nuevas propias de compras: **crédito fiscal condicional** y **percepciones sufridas**.

**Infraestructura (V21):**
- `Proveedor.condicionIva` (`RESPONSABLE_INSCRIPTO` / `MONOTRIBUTISTA` / `EXENTO` / `CONSUMIDOR_FINAL`, default RI) y `Proveedor.cuentaCxp` (FK opcional a `CuentaContable`, mismo criterio que `Cliente.cuentaCxc` de F4.2 — checkpoint F4.1 §2.2, mantener cuentas por proveedor).
- `mapeo_cuenta` seed para los 5 conceptos que ya tenían cuenta fija en el seed F3.3 (`IVA_CREDITO_FISCAL`→1.1.2006, `PERCEPCION_IVA_SUFRIDA`→1.1.2007, `PERCEPCION_IIBB_SUFRIDA`→1.1.2008, `RETENCION_GANANCIAS_SUFRIDA`→1.1.2011, `RETENCION_IVA_SUFRIDA`→1.1.2007). **`COSTO_GASTO`** (discriminado por categoría/`tipo_costo`, dato de usuario sin seed propio) y **`DEUDA_COMERCIAL`** (sin fila por defecto a propósito, mismo criterio que `CREDITO_POR_VENTA` en F4.2) quedan para que el admin los configure vía `/mapeos-cuenta` una vez creados sus `tipo_costo`/cuentas por proveedor — verificado en vivo que sin esa configuración la confirmación falla con `MAPEO_CUENTA_FALTANTE` claro, no un error críptico.
- `factura_compra` + `factura_compra_linea`: mismo patrón de `factura_venta`/`factura_venta_linea`, con `proveedor_id` en vez de `cliente_id` y `tipo_costo_id` (FK a `TipoCosto`, F2.3) en la línea en vez de `tipo_ingreso`.

**Dominio `FacturaCompra`:**
- `FacturaCompraAsientoGenerator`: por línea, resuelve `COSTO_GASTO` vía `ResolutorCuentas` discriminado por `tipoCosto.nombre()` (o usa el override de cuenta de la línea); agrega una línea `IVA_CREDITO_FISCAL` agregada si el comprobante computa crédito fiscal; agrega una línea por cada tributo `PERCEPCION_IVA`/`PERCEPCION_IIBB` sufrido (leídos de `comprobante_tributo`, tabla genérica ya creada en F4.2); cierra con `DEUDA_COMERCIAL` (cuenta propia del proveedor o fallback al mapeo). Notas de crédito/débito invierten debe/haber (ADR-13), igual que en F4.2.
- **Crédito fiscal condicional** (F4.1 §5): `computaCreditoFiscal = tipoComprobante != FACTURA_C && proveedor.condicionIva == RESPONSABLE_INSCRIPTO`. Si es falso, cada línea de costo debita `neto + iva` (el IVA discriminado se absorbe en el costo, sin línea de IVA CF separada) — verificado que dos condiciones independientes (proveedor no-RI, o comprobante tipo C con proveedor RI) ambas fuerzan el mismo comportamiento.
- **Percepciones sufridas**: `FacturaCompraTributoRequest` valida en el service que solo `PERCEPCION_IVA`/`PERCEPCION_IIBB` son aceptables para una factura de compra (`TRIBUTO_NO_APLICABLE_A_COMPRA` para cualquier otro tipo — Montanari no es agente de retención, checkpoint F4.1 #3); el generador tiene el mismo chequeo como defensa en profundidad por si algún tributo de otro tipo llega a persistirse. Se guardan en `comprobante_tributo` (comprobanteTipo=`FACTURA_COMPRA`), reemplazadas por completo en cada `crear`/`editar` (mismo patrón que las líneas).
- `FacturaCompraService`: ciclo de vida idéntico a `FacturaVentaService` (`crearBorrador`/`editarBorrador`/`eliminarBorrador`/`confirmar`/`anular`, delega en `AsientoService.registrarAutomatico`/`anularPorDocumento`), con la validación de alícuota IVA (0/2.5/5/10.5/21/27) reusada tal cual.
- Frontend: `facturas-compra-page.tsx` (mismo patrón de `asientos-page.tsx`/`facturas-venta-page.tsx`: cabecera+líneas con `useFieldArray`, más un segundo `useFieldArray` para las percepciones sufridas), `proveedores-page.tsx` extendido con los selectores de condición de IVA y cuenta CxP propia.

## Decisión de diseño no especificada literalmente

La entrada de la nota de "retenciones" en el texto de F4.1 §5 ("percepciones sufridas, retenciones, total") no tiene contraparte en la tabla de líneas de esa misma sección (solo lista `PERCEPCION_IVA_SUFRIDA`/`PERCEPCION_IIBB_SUFRIDA`) ni tiene sentido de negocio: en una factura de compra Montanari es el comprador, así que una "retención sufrida" ahí implicaría que el proveedor le retiene a Montanari, lo cual no ocurre — y "retención practicada" (Montanari reteniendo al proveedor) está descartada por el checkpoint #3 (Montanari no es agente de retención). Se interpretó esa mención como un artefacto de redacción y **no se implementó ninguna línea de retención en el generador de compra** — consistente con la tabla de líneas de F4.1 §5 y con el checkpoint. Las retenciones sufridas sí tienen su lugar correcto: en el **cobro** de una factura de venta (F4.4 §6.1), donde Montanari es quien cobra y el cliente es quien retiene.

## Verificación realizada

- **Compilación** dentro de contenedor `maven:3.9-eclipse-temurin-21`: limpia.
- **19 tests nuevos**: `FacturaCompraAsientoGeneratorTest` (FC-1 ARS con crédito fiscal, FC-2 USD sin IVA con fallback de CxP, FC-3 ARS con percepción de IVA sufrida, FC-4 proveedor monotributista sin crédito fiscal, folding de IVA con alícuota discriminada, comprobante tipo C fuerza no-crédito-fiscal aunque el proveedor sea RI, inversión de signo en nota de crédito, `MAPEO_CUENTA_FALTANTE`, `TRIBUTO_NO_APLICABLE_A_COMPRA` como defensa en profundidad, `FACTURA_SIN_IMPORTE`), `FacturaCompraServiceTest` (cálculo de totales incluyendo percepciones, validación de alícuota y de tributo no aplicable, ciclo de vida completo, transiciones inválidas, anulación cascada). Además se actualizaron los tests de `ProveedorServiceTest` por la ampliación de sus DTOs.
- **210/211 tests** en la suite completa (el único que falla es el Testcontainers/Docker Desktop local ya conocido, ver [[build-env-jdk21-testcontainers]]); antes de esta suite hubo un hiccup transitorio del daemon de Docker Desktop (`failed to connect... npipe`) que se resolvió solo al reintentar, sin relación con el código.
- **Verificación end-to-end contra el servidor real** (`docker compose up`, migraciones V1→V21 desde volumen limpio; se usaron puertos temporales 3308/8083 porque el host ya tenía otros proyectos ocupando el 3307 — revertido a 3307/8081 en el archivo commiteado):
  - FC-1 (ARS, proveedor responsable inscripto con cuenta CxP propia): asiento de 3 líneas, debe=haber=121.000,00, resuelve Costo Programador (5.1.2002), IVA Crédito Fiscal (1.1.2006) y la cuenta CxP propia del proveedor (2.1.2002).
  - Confirmar una factura de proveedor **sin** cuenta CxP propia y **sin** mapeo por defecto de `DEUDA_COMERCIAL` → `422 MAPEO_CUENTA_FALTANTE` claro, la factura queda en BORRADOR (rollback transaccional confirmado); tras crear el mapeo por defecto vía `/mapeos-cuenta`, la misma confirmación resuelve correctamente y foldea el IVA discriminado en la línea de costo (proveedor monotributista, sin línea de IVA CF separada).
  - FC-3 con una percepción de IVA sufrida: asiento de 4 líneas balanceado (100.000 + 21.000 + 3.000 = 124.000 = CxP).
  - FC-2 estilo USD sin IVA: `totalArs` = 200 × 1.500 = 300.000,00 correcto; asiento preserva moneda/importe original/TC en ambas líneas.
  - Nota de crédito A: débito/crédito invertidos respecto de una factura normal, balance exacto.
  - Anulación en cascada: anular la factura pone en ANULADO tanto la factura como su asiento vinculado; se confirmó que `/asientos/{id}/anular` directo sigue rechazando ese mismo asiento con `ANULACION_VIA_DOCUMENTO`.
  - Adjuntos: subida multipart, listado y descarga contra `entidadTipo=FacturaCompra` funcionan igual que en F4.2.
  - Permisos: usuario `LECTURA` real recibe 403 al crear proveedor, factura de compra o mapeo de cuenta, y 200 en las lecturas equivalentes.
- Frontend: `tsc -b` y `oxlint` limpios sobre `facturas-compra-page.tsx`, `proveedores-page.tsx` y el wiring de router/nav (solo 2 warnings preexistentes en componentes UI compartidos, no relacionados).
