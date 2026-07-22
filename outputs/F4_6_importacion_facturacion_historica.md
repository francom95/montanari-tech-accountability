# F4.6 — Importación de facturación histórica

**Paso:** 27 de 55 · **Fase:** F4 — Facturación, cobros y pagos · **Modelo:** Sonnet 5 · **Depende de:** F4.3
**Checkpoint humano:** No.

## Cambio de alcance pedido por el usuario

El plan de este paso preveía Excel/CSV como entrada principal y PDF solo como "extracción básica + formulario de carga asistida". El usuario adjuntó 7 comprobantes reales de Montanari Tech (facturas de venta y compra, incluyendo layouts de terceros como Mercado Pago, OpenAI y Dattatec/DonWeb) y pidió explícitamente invertir el orden: **PDF como entrada principal, sin Excel/CSV**. Se implementó así — el importador de F4.6 es 100% PDF.

## Qué se hizo

**`ExtractorFacturaPdf`** (`facturacion/importacion`): extracción básica de texto vía PDFBox + un conjunto de expresiones regulares sobre ese texto lineal — explícitamente **no** es un parser de layout (eso es la "etapa 2" que el propio paso descarta). Reconoce el layout estándar ARCA/AFIP (usado por la mayoría de los comprobantes reales de Montanari) y cae con gracia a completado manual para layouts propios de terceros (Mercado Pago, OpenAI, Dattatec) que no siguen ese estándar. Campos que intenta resolver: tipo de comprobante (código ARCA), punto de venta/número, fecha, CUIT de la contraparte, moneda, tipo de cambio, neto/alícuota de IVA (con 3 caminos: desglose por alícuota de Factura A, "IVA Contenido" de Factura B/C, o SUBTOTAL de layouts propios), total y CAE. **Venta vs. compra** se decide comparando el CUIT emisor contra el CUIT propio de Montanari (`app.empresa.cuit`, ya configurado); cualquier campo no resuelto queda `null` con una advertencia — **nunca se confirma nada automáticamente sin revisión**.

**`ImportacionFacturaService`** — flujo de dos fases:
1. `previsualizar(List<MultipartFile>)`: extrae texto + campos de cada PDF, intenta resolver cliente/proveedor existente por CUIT, devuelve una fila de previsualización por archivo con todo lo detectado + advertencias. Nada se persiste todavía.
2. `confirmar(List<FilaImportacionConfirmarRequest>)`: recibe las filas ya revisadas/corregidas por el usuario (incluye alta rápida de cliente/proveedor si no existía) y por cada una: resuelve o crea el cliente/proveedor, valida idempotencia (mismo cliente/proveedor + tipo + punto de venta + número — la misma unique constraint de F4.2/F4.3), y llama a **los servicios ya existentes** `FacturaVentaService.crearBorrador`/`FacturaCompraService.crearBorrador` (nunca inserta directo). Si `estadoDestino=CONFIRMADO`, intenta además `confirmar(...)`; si falla (ej. `MAPEO_CUENTA_FALTANTE`), la factura **queda como borrador con una advertencia explicando por qué**, no se pierde el trabajo de carga. Cada fila se procesa en un try/catch propio — una fila rota no aborta el resto del lote.

**Idempotencia**: `existsBy{Cliente|Proveedor}IdAndTipoComprobanteAndPuntoVentaAndNumero` antes de crear — re-importar el mismo PDF (o el mismo comprobante en otro PDF) se rechaza con "Ya importada", nunca duplica.

**Endpoints** (`ImportacionFacturaController`, rol ADMINISTRADOR/CARGA salvo el de exportar rechazos que no requiere lote persistido):
- `POST /importacion-facturas/pdf/previsualizar` (multipart, N archivos) → lista de previsualización.
- `POST /importacion-facturas/confirmar` (lista de filas revisadas) → lista de resultados (éxito/rechazo).
- `POST /importacion-facturas/rechazos/exportar/excel` (recibe la lista de rechazos del paso anterior en el body, sin persistir un "lote de importación" como entidad — más simple, coherente con que esto es un importador y no un documento con estados propios) → reusa `ReportExportService` (mismo servicio de F4.5).

**Frontend** (`importacion-historica-page.tsx`): sube PDFs → previsualiza → una tarjeta editable por factura (tipo, comprobante, cliente/proveedor o alta rápida, moneda/TC, línea, destino borrador/confirmado) con las advertencias de extracción visibles → confirmar → tabla de resultados + descarga de rechazos.

## Bugs reales encontrados y corregidos contra los 7 PDFs reales

El extractor se escribió primero contra texto "idealizado" (reconstruido a mano según el layout visual). Al correrlo contra los PDFs reales del usuario aparecieron 3 discrepancias genuinas entre cómo PDFBox extrae el texto y cómo se ve visualmente el comprobante — las 3 corregidas y **los tests reescritos con el texto real capturado del endpoint** (no más texto idealizado, para que sean una protección real contra regresiones):

1. **Etiquetas pegadas, valores pegados, en otro orden.** El layout estándar ARCA/AFIP muestra visualmente "Punto de Venta: 00003  Comp. Nro: 00000105", pero PDFBox extrae "Punto de Venta: Comp. Nro:00003 00000105" (las dos etiquetas juntas, después los dos valores juntos) — afectó 3 de 4 comprobantes estándar. Se agregó un patrón alternativo para esta variante.
2. **Valor pegado a la palabra siguiente sin espacio.** Un CAE aparecía como "86262004442157Pág. 1/1" — `\b(\d{14})\b` no matchea porque dígito→letra no es un límite de palabra en regex Java (ambos son `\w`). Se cambió a `(?<!\d)(\d{14})(?!\d)`, que solo exige que no haya OTRO dígito pegado.
3. **Espacio de no separación (U+00A0) en vez de espacio normal.** Un total aparecía como `"TOTAL $ 18828.00"` — `\s` de Java no matchea U+00A0. Se normaliza ` `→espacio una sola vez al principio de `extraer()`, en vez de parchear cada patrón individual.

**Límite real y aceptado, no corregido**: en el comprobante de exportación (Jarp Inc, USD), el valor del total aparece **pegado antes** de su etiqueta ("483,00Importe Total:"), sin ningún separador — recuperar esto exigiría heurísticas de layout más allá de "extracción básica de texto + regex", exactamente lo que el paso reserva para la etapa 2. Queda como advertencia visible; el usuario completa el total a mano en el formulario asistido.

## Verificación realizada

- **Compilación** y **suite completa** dentro de contenedor `maven:3.9-eclipse-temurin-21`: limpia. 267/268 tests (el único que falla es el Testcontainers/Docker Desktop local ya conocido).
- **7 tests del extractor**, ahora con el texto real capturado del endpoint (no reconstruido a mano) para cada uno de los 7 PDFs del usuario — cubren el layout estándar con/sin desglose de IVA, exportación en USD, y los 3 layouts de terceros (Mercado Pago, OpenAI en inglés, Dattatec).
- **6 tests del servicio de confirmación**: alta rápida, idempotencia, fallback a borrador cuando falla la confirmación automática, rechazo cuando falta `tipoCostoId` en una compra, aislamiento de fallas por fila.
- **Verificación end-to-end contra el servidor real, con los 7 PDFs reales del usuario** (no con texto de prueba): se subieron los 7 archivos al endpoint de previsualización real y se confirmaron los 3 bugs de extracción de arriba contra los PDFs originales (antes y después del fix). Además:
  - Confirmación de una compra con alta rápida de proveedor nuevo (Lubenfeld) como borrador — proveedor creado con CUIT y jurisdicción correctos.
  - Re-envío del mismo comprobante → rechazado con "Ya importada" (idempotencia real, no solo en test).
  - Confirmación de una venta pidiendo `estadoDestino=CONFIRMADO` sin tener mapeada la cuenta de CxC del cliente nuevo → quedó como borrador con la advertencia exacta del error (`MAPEO_CUENTA_FALTANTE`), sin perder la carga.
  - Exportación de rechazos a Excel: archivo `.xlsx` válido.
  - Permisos: usuario `LECTURA` real recibe 403 tanto en previsualizar como en confirmar.
- **Frontend**: `tsc -b` y `oxlint` limpios. La verificación visual en navegador no fue posible contra el backend de esta corrida por el gap de CORS ya documentado en el proyecto (puerto de verificación temporal ≠ origen configurado) — cubierto en cambio por la verificación exhaustiva a nivel API descripta arriba, incluyendo los 7 PDFs reales del usuario.
