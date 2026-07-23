# F7.2 — Balance de sumas y saldos

**Paso:** 36 de 55 · **Fase:** F7 — Reportes y dashboard · **Modelo:** Sonnet 5 · **Depende de:** F7.1
**Checkpoint humano:** No.

## Qué se hizo

Reporte con todas las cuentas del plan (F3.2), agrupadas por jerarquía (madre = Σ hijas), con verificación de integridad Σdebe = Σhaber del período y drill-down al Mayor (F3.6) de cada cuenta con los mismos filtros de período.

### Fuente de datos: una query agregada nueva, no una por cuenta

`AsientoLineaRepository.sumarDebeHaberPorCuenta(fechaDesde, fechaHasta, CONFIRMADO)` — una sola consulta `GROUP BY cuenta` que trae el debe/haber acumulado de **todas** las cuentas imputables con movimiento en el período, en vez de repetir la consulta puntual de `MayorService` una vez por cuenta. Solo cuentas imputables aparecen (F3.4 exige `CUENTA_NO_IMPUTABLE` al confirmar una línea), así que las cuentas madre nunca tienen movimientos propios que sumar.

### El roll-up: madre = Σ hijas, en cualquier profundidad

`BalanceSumasYSaldosService` arma el árbol con la misma técnica que `CuentaContableService.arbol()` (agrupar por `padre_id` en memoria, plan de cuentas acotado) y calcula el debe/haber de cada cuenta madre como la suma de sus hijos ya calculados — recursivo, sin importar cuántos niveles tenga la rama (el plan de cuentas real llega a nivel 5 en algunas ramas, ver V17). Verificado en el E2E con una jerarquía de 3 niveles real.

### Verificación de balanceo global — nunca se oculta

`totalDebe`/`totalHaber`/`balancea`/`diferencia` se calculan **siempre sobre el total sin filtrar**, independientemente de `incluirSinMovimiento`/`nivelMaximo` (que solo deciden qué se muestra en pantalla). Si no balancea — algo que en teoría nunca debería pasar, porque todo asiento confirmado ya balancea individualmente (PL-4/PL-5) — el frontend muestra un banner de error destacado con la diferencia exacta, tanto en pantalla como en la fila de totales del export. Es una señal de bug real (corrupción de datos, un asiento que se coló sin pasar por el service), nunca un caso de negocio a tolerar.

### Filtros: sin movimiento y nivel de jerarquía

- **Incluir/excluir sin movimiento** (default: excluir): una cuenta con debe=haber=0 se poda del árbol junto con todos sus ancestros que también queden en cero, recursivamente.
- **Nivel máximo**: corta la visualización en el nivel pedido (el nodo cortado no muestra hijos), pero el debe/haber de ese nodo sigue siendo el roll-up **completo** de todo su subárbol — cortar la vista nunca pierde el total.

### Drill-down con los mismos filtros de período

El link "Ver mayor" de cada cuenta imputable navega a `/contabilidad/mayor/:cuentaId?fechaDesde=...&fechaHasta=...`. `MayorPage` (F3.6) ahora lee esos query params al montar para prellenar sus propios filtros de fecha — cambio mínimo y acotado a esa página, necesario para que el drill-down realmente lleve el mismo período (antes esos filtros solo vivían en estado local del componente).

### Exportable (F7.1)

Reusa `ReportExportService`/`ContextoReporte` tal cual: el árbol se aplana en pre-order con indentación por nivel en la columna "Cuenta", y se agrega una fila final de totales con el resultado del balanceo — visible también en el archivo, no solo en pantalla.

## Verificación realizada

- **Suite completa** en `maven:3.9-eclipse-temurin-21`: **440/441 en verde** (el único fallo es el de Testcontainers/Docker Desktop local, esperado). **9 nuevos** en `BalanceSumasYSaldosServiceTest`: roll-up en 2 y 3 niveles, balancea/no balancea con diferencia expuesta, incluir/excluir sin movimiento, corte por nivel máximo preservando el total, `contrarioAlEsperado`, cuenta imputable sin ningún movimiento.
- Frontend: `tsc -b` y `oxlint` limpios.
- **E2E contra MySQL 8 real** (docker-compose): plan de cuentas de 3 niveles creado vía API (Activo → Activo Corriente → Caja/Banco), dos asientos confirmados (aporte de capital, transferencia interna Caja→Banco). Balance resultante: `totalDebe = totalHaber = 130.000`, `balancea = true`; Caja 70.000 / Banco 30.000 confirmados; Activo Corriente (madre) y Activo (raíz, 2 niveles arriba) reproducen el roll-up exacto; filtro sin-movimiento oculta cuentas sin actividad en el período pedido y las muestra con `incluirSinMovimiento=true`; `nivelMaximo=2` corta la vista sin perder el total. Export Excel y PDF verificados con contenido real.
- Verificación visual en browser: bloqueada por una limitación de red del sandbox del preview (no llega al puerto publicado del backend aunque el propio host sí), no relacionada con el código. La corrección funcional ya queda probada de punta a punta por el E2E con `curl` contra datos reales.
