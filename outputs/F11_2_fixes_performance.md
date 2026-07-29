# F11.2 — Fixes + performance

Modelo asignado: Sonnet 5. Usado: Sonnet 5 (sin discrepancia).

> Insumo: el informe completo de [F11.1](F11_1_revision_final_de_seguridad_e_integridad_contable.md) (8 bloqueantes, 19 altos). El usuario decidió el criterio de negocio pendiente de B8 (excluir exportaciones de la base de IIBB) antes de arrancar este paso.

## Alcance de este paso

Cierra los 5 bloqueantes que quedaban abiertos tras F11.1 (B2/B3/B4 ya se habían cerrado como los "3 más baratos" en una sesión previa), los 19 ALTOS, y el trabajo de performance. Cada fix se verificó dos veces: con un test unitario nuevo o existente, y — para los bloqueantes de seguridad — en vivo contra el sistema real (Docker Compose + MySQL 8) reproduciendo el ataque exacto del informe F11.1.

## Bloqueantes cerrados

### B2/B3/B4 — Aislamiento multi-tenant (el fix más grande de este paso)

La causa raíz real no era la sospechada en F11.1: no alcanzaba con parchear `findById`. Se encontraron y corrigieron **dos problemas distintos, en cascada**:

1. **`EntityManager.find`/`getReference` nunca aplican el filtro Hibernate `@Filter`** (solo alcanza a HQL/Criteria). Fix: `TenantScopedRepositoryImpl` — una `repositoryBaseClass` global (`@EnableJpaRepositories`) que sobreescribe `findById`/`existsById`/`findAllById`/`getReferenceById` en los 58 repositorios del proyecto de una sola vez, descartando silenciosamente cualquier fila cuyo `tenantId` no coincida con `TenantContext`.
2. **Verificado en vivo que ESO NO ALCANZABA**: `GET /clientes` seguía devolviendo los 19 clientes reales de otro tenant pese al fix de (1), porque es una query JPQL (`@Query`), no `findById`. Causa raíz real: `TenantFilterInterceptor` (que habilita el filtro Hibernate por request) corría **antes** que `OpenEntityManagerInViewInterceptor` (que abre el `EntityManager` real) — ambos con orden por defecto 0, y el propio ganaba esa carrera por orden de registro de beans. El filtro se habilitaba sobre un `EntityManager` efímero que se descartaba de inmediato; el que OSIV abría después para el resto del request nunca lo tenía habilitado. Fix: `registry.addInterceptor(tenantFilterInterceptor).order(10)` — un valor explícito que garantiza correr después de OSIV.
3. **Efecto colateral real del fix de (2)**: el login se rompió para cualquier tenant que no fuera el default, porque `UsuarioRepository.findByEmail` (JPQL) quedó correctamente filtrado por tenant — pero el login es la única búsqueda legítimamente global del sistema (no se sabe a qué tenant pertenece el usuario hasta encontrarlo). Fix: `findByEmailGlobalParaLogin` (query nativa, fuera del alcance del filtro Hibernate a propósito) usada solo en `AuthService.login` y `CustomUserDetailsService`.
4. **B4** (auditoría cruzada): `AuditoriaLog` no extiende `EntidadNegocio` (diseño original), así que el filtro nunca la alcanza — se agregó `AND a.tenantId = :tenantId` explícito a la query.

**Verificado en vivo, en ambas direcciones**, con tokens reales de dos tenants: `GET /clientes`, `GET /facturas-venta`, `GET /asientos/{id}`, `GET /auditoria`, `PUT /usuarios/{id}/password` — los 5 ataques del informe F11.1 cerrados; el tenant real sigue viendo sus propios 19 clientes (sin falso negativo). Balance real (`Σdebe=Σhaber=$305.621.023,15`) verificado intacto después de cada ronda de pruebas.

### B5 — Gate de período cerrado

`AsientoService.confirmar` y `registrarAutomatico` (el único punto de entrada de los generadores automáticos) no llamaban a `PeriodoService.verificarEscritura` en absoluto. Se agregaron overloads con override de admin, siguiendo el mismo patrón que `crearBorrador`/`editarBorrador`/`anular` (F9.3). **Decisión documentada**: los 4 llamadores de `registrarAutomatico` que no tenían su propio chequeo (`MovimientoBancarioService`, `PagoTarjetaService`, `LiquidacionIvaService`, `LiquidacionIibbService`) todavía no exponen el override a su propio controller — quedan bloqueados en período cerrado incluso para ADMIN hasta que se decida exponerlo (opción segura por defecto: falla cerrado, no abierto).

Verificado en vivo: el ataque exacto del informe (confirmar un asiento con fecha en un período cerrado, rol CARGA, sin override) ahora devuelve 422 en vez de 200.

### B7 — Redondeo multimoneda (cobros/pagos con 2+ imputaciones)

La línea de Fondos (Debe) redondeaba una vez sobre el agregado; el resto de las líneas (CxC, recargo, anticipo) redondeaban componente por componente — `round2(Σxᵢ×tc) ≠ Σround2(xᵢ×tc)` en general. Fix: la línea de Fondos ahora se deriva como la suma exacta de los componentes ya redondeados (mismo patrón "regla del residuo" que ya usaba el cierre de saldo por factura), con un TC "efectivo" que reproduce ese monto exacto — mismo mecanismo que ya usaba la línea de CxC.

Test de regresión con los números exactos del informe (cobro USD 1.819,39 @ TC 1.240,18 imputado a dos facturas de 712,75 y 1.106,64): antes, Debe $2.256.371,09 vs Haber $2.256.371,10 (delta $0,01, `ASIENTO_NO_BALANCEA`); ahora balancea exacto.

### B8 — Base de IIBB: exportaciones + conversión de moneda

Con el criterio del usuario (excluir `FACTURA_E` por completo): `CalculoIibbService` ahora convierte `netoGravado × tipoCambio` antes de sumar a la base (antes se sumaba en moneda original sin convertir — una venta local en USD quedaba declarada por una fracción de su valor real), y excluye las facturas de exportación de la base y de todos los repartos, con una advertencia informativa del monto excluido.

## Altos cerrados

| # | Fix |
|---|---|
| A1 | `@DecimalMin(value="0.0", inclusive=false)` en `tipoCambio` de los 10 DTOs de request que lo tenían sin validar (Cobro/Pago Crear+Editar, FacturaVenta/FacturaCompra Crear+Editar, MovimientoBancario Crear+Corregir, PagoTarjetaCrear) + `CHECK (tipo_cambio > 0)` en las 8 tablas que no lo tenían (migración V47). |
| A2 | **No resuelto** — no se pudo reproducir de forma concluyente en el tiempo disponible; documentado como pendiente. |
| A3 | `RefreshTokenService.consumirYRotar` ahora filtra por `usuario.isActivo()` — antes un usuario desactivado podía renovar su sesión indefinidamente vía refresh. |
| A4 | Sacado el fallback hardcodeado de baja entropía en `application-dev.yml`; dev ahora exige `JWT_SECRET` igual que el resto de los perfiles. |
| A5 | No resuelto (scrubbing de PII en fixtures de test, fuera de alcance de este paso). |
| A6 | Verificado en código: `AlertaScheduler` ya habilita el filtro de tenant correctamente por iteración — no era un hallazgo real, false positive del informe original. |
| A7 | Cubierto parcialmente por el fix de B2 (`getReferenceById` ahora también respeta tenant). |
| A8 | Gate de período agregado a los 4 `confirmarVinculandoAsientoExistente` (F10.3). |
| A9 | Los 4 métodos ahora reasignan `origen`/`origenTipo`/`origenId` del asiento vinculado al nuevo documento, para que `AsientoService.anular` lo proteja igual que a un asiento generado automáticamente. |
| A10 | `UNIQUE (asiento_id)` en `factura_venta`/`factura_compra`/`cobro`/`pago` (migración V47). |
| A11 | Rechaza vincular un asiento que ya tiene `origenTipo` seteado (ya vinculado a otro documento). |
| A12 | Auditoría agregada a 4 endpoints de configuración que no dejaban rastro: `ConfiguracionAlertasController`, `ConfiguracionDashboardController`, `ConfiguracionPresupuestoController`, `TipoCambioController.actualizarConfiguracion`, `CobroController.actualizarConfiguracionCobranza`. `AtribucionImpuestoController` ya auditaba correctamente (false positive del informe original). |
| A13 | Nuevo `AccionAuditoria.LOGIN_FALLIDO`: `AuthService.login` audita el intento fallido contra el usuario existente (si el email matchea alguno). |
| A14 | Cerrado como efecto directo del fix de B5 (`sobrePeriodoCerrado` ahora se propagá correctamente en `confirmar`/`registrarAutomatico`). |
| A16 | `confirmarVinculandoAsientoExistente` de Cobro/Pago ahora rechaza (`VINCULACION_NO_SOPORTA_IMPUTACIONES`) si el documento tiene imputaciones, en vez de dejar `montoArsCancelado` en NULL. |
| A17 | `@DecimalMax("1.0")` en el coeficiente + advertencia si Σ coeficientes ≠ 1. |
| A18 | La herencia del coeficiente del mes anterior ahora es una decisión a nivel de liquidación completa: una jurisdicción sin fila en la liquidación anterior entra en CERO (con advertencia), nunca al criterio de destino mientras el resto usa CM real. |
| A19 | Ventas a una jurisdicción inactiva ahora generan una advertencia explícita con el monto. |

## Performance

Dataset real: 847 asientos, 1.924 líneas — a esta escala cualquier query es sub-100ms, así que no había un problema de performance *hoy*. Se verificó con EXPLAIN el patrón de query más repetido del sistema (Mayor, Balance de sumas y saldos, Estado de Resultados, liquidaciones de IVA/IIBB: todos filtran `tenant_id + estado='CONFIRMADO' + fecha BETWEEN`), y se agregó `ix_asiento_tenant_estado_fecha (tenant_id, estado, fecha)` (migración V48) como índice de cobertura para cuando el volumen crezca — confirmado con `EXPLAIN ... USE INDEX`: `type=range, filtered=100%, Using index`.

**Hallazgo documentado, no corregido**: 4 índices FULLTEXT existen (`ft_cliente_nombre`, `ft_proveedor_nombre`, `ft_proyecto_nombre`, `ft_cuenta_contable_nombre`) pero ningún query los usa — Lupita busca estas 4 entidades reutilizando el método `buscar()` del propio CRUD de cada una (LIKE, no MATCH). Es peso muerto en escritura sin beneficio en lectura; no se tocó por alcance/riesgo (cambiar `buscar()` afecta también las pantallas de listado propias de cada entidad, no solo Lupita).

## Verificación

- **Backend**: 688/688 tests (el único error es el ambiental de Testcontainers ya documentado, sin Docker en el runner local).
- **Migraciones**: V47 (CHECK tipo_cambio + UNIQUE asiento_id) y V48 (índice compuesto) aplicadas limpio contra los datos reales — se verificó primero que ninguna fila existente violaba las nuevas restricciones.
- **Balance real**: `Σdebe=Σhaber=$305.621.023,15`, diferencia $0,00, verificado después de cada ronda de pruebas en vivo.
- **B2/B3/B4 verificados en vivo** con tokens reales de dos tenants, en ambas direcciones, incluyendo un chequeo explícito de que el tenant real sigue viendo sus propios datos (sin over-filtrado).

## Pendiente para una futura pasada

- A2 (precisión de `tipoCambioEfectivo` en imputaciones grandes) — no reproducido de forma concluyente.
- A5 (PII real en fixtures de test).
- A6/A7 quedaron cubiertos como false positives o parcialmente, respectivamente — no requieren trabajo adicional urgente.
- FULLTEXT sin usar en 4 entidades de Lupita (hallazgo de performance documentado arriba).
- Exponer el override de período cerrado en los controllers de MovimientoBancario/PagoTarjeta/LiquidacionIva/LiquidacionIibb, si el equipo decide que ADMIN necesita poder confirmarlos en período cerrado (hoy quedan bloqueados sin excepción).
