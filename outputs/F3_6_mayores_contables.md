# F3.6 — Mayores contables

**Paso:** 21 de 55 · **Fase:** F3 — Núcleo contable · **Modelo:** Sonnet 5 · **Depende de:** F3.4 · **Plantilla:** PL-3
**Checkpoint humano:** No.

## Qué se hizo

Sobre el motor de asientos (F3.4/F3.5), se implementó el mayor de cada cuenta (F3.1 §5), aplicando el molde PL-3 (`ReportExportService`, ya embrionario desde F1.8):

- **`GET /api/v1/cuentas-contables/{id}/mayor`**: mayor paginado para pantalla, con los 8 filtros pedidos (fecha desde/hasta, rubro, proyecto, cliente, proveedor, origen/tipo de operación, moneda). Solo considera asientos `CONFIRMADO` (ADR-04: ningún saldo está persistido, todo se deriva por consulta).
- **`GET .../mayor/exportar/excel`** y **`.../exportar/pdf`**: la misma consulta sin paginar, en streaming.
- **Acceso desde el árbol**: botón "Ver mayor" en cada nodo de `plan-de-cuentas-page.tsx`, navega a `/contabilidad/mayor/:cuentaId`.
- **Mayor de cuenta madre** (F3.1 §5.3): agrega todas las imputables descendientes. La resolución del árbol es en Java (BFS sobre `CuentaContableRepository.findByPadreId`), no un CTE recursivo en SQL — a la escala de esta empresa (~70 cuentas, pocos niveles) es más simple y perfectamente suficiente, y evita introducir el primer query nativo/recursivo del proyecto.
- **Saldo anterior** (CP-17): cuando hay `fechaDesde`, una fila sintética inicial resume todo lo confirmado antes de esa fecha.
- **Vista analítica** (F3.1 §5.4): cualquier filtro dimensional (rubro/proyecto/cliente/proveedor/origen/moneda) rotula el resultado como "saldo del filtro" en vez de "saldo de la cuenta", y suprime la comparación contra `saldoEsperado` (no tendría sentido comparar un subconjunto filtrado contra el saldo esperado de toda la cuenta).
- **Saldo contrario al esperado** (CP-20): el mayor expone `contrarioAlEsperado` como dato informativo — nunca bloquea nada (es un endpoint de lectura, no hay nada que bloquear). La emisión de la alerta real es del motor de F9.1, todavía no implementado.

## Decisiones tomadas (sin checkpoint, documentadas para trazabilidad)

1. **Cálculo completo en memoria, paginado después.** En vez de repartir el acumulado entre SQL y aplicación (saldo antes del offset + página + running sum), se trae la lista completa de líneas que matchean el filtro, se calcula el acumulado una sola vez en Java, y recién ahí se recorta la página pedida. Es más simple y menos propenso a errores que partir el cálculo, y a esta escala (una sola empresa, un puñado de miles de movimientos por cuenta como mucho) no es un problema de performance real. La exportación reusa el mismo cálculo completo sin necesidad de recalcular.
2. **Bug encontrado y corregido durante la verificación:** la consulta de "saldo anterior" originalmente devolvía `SUM(debe), SUM(haber)` como una tupla (`Object[]`) en una sola query. Spring Data JPA envuelve las proyecciones multi-columna como `List<Object[]>`, no `Object[]` directo, así que el cast fallaba en runtime (`ClassCastException`) — invisible en los tests unitarios (Mockito no ejecuta JPQL real) y solo detectado al levantar el backend contra MySQL real. Se resolvió separando en dos consultas escalares (`sumarDebeAntesDeFecha`/`sumarHaberAntesDeFecha`), cada una devolviendo `BigDecimal` directo sin ambigüedad.
3. **Sin migración nueva.** El mayor es puramente de lectura sobre columnas que ya existen (`asiento`, `asiento_linea`, `cuenta_contable`); los índices de F3.4 (`ix_asiento_linea_cuenta`, `ix_asiento_tenant_fecha`) ya cubren los filtros más comunes.

## Verificación realizada

- **7 tests nuevos** (`MayorServiceTest`) cubriendo CP-17 (con y sin `fechaDesde`, saldo anterior + acumulado), CP-20 (saldo contrario al esperado, no bloquea), vista analítica, agregación de madre sobre descendientes (incluye el caso "madre sin descendientes" para no romper con un `IN ()` vacío), y paginación.
- **173/174 tests** en la suite completa (el único que falla es el Testcontainers/Docker Desktop local ya conocido).
- **Verificación end-to-end contra el servidor real** (`docker compose up`, cadena completa V1→V19 desde un volumen limpio): se cargaron 2 asientos reales (depósito + pago) sobre "Banco Galicia CC", y se probó:
  - Mayor sin filtro: acumulado 500.000 → 300.000, saldo final DEUDOR.
  - Mayor con `fechaDesde`: fila "Saldo anterior" = 500.000, resto del acumulado correcto (**acá se encontró y corrigió el bug del punto 2**).
  - Mayor de la cuenta madre (1.1 Activo Corriente): agrega correctamente el Banco y sus hermanas sin movimientos.
  - Vista analítica con filtro de rubro: `vistaAnalitica: true`.
  - Export Excel → archivo `.xlsx` válido (`Microsoft Excel 2007+`, verificado con `file`); export PDF → header `%PDF-1.5` válido.
  - Permisos: usuario `LECTURA` real puede consultar y exportar el mayor (200 en ambos), conforme a la matriz de F3.1 §4.6 ("Ver, buscar, exportar asientos y mayores: todos los roles").
- Frontend: `tsc -b` limpio.
