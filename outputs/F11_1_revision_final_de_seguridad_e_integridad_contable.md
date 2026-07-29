# F11.1 — Revisión final de seguridad e integridad contable

Modelo asignado: Fable 5. **Usado: Opus 5** (discrepancia planteada al usuario, que eligió Opus 5 — mismo precedente que F4.5/F9.1/F9.2/F10.2).

> ⚠️ **Este paso tiene checkpoint humano**: el equipo decide qué hallazgos bloquean el go-live. Este documento no decide, informa.

---

## Veredicto

**El sistema no está en condiciones de go-live.** Se encontraron **8 hallazgos BLOQUEANTES**, de los cuales **7 fueron reproducidos o cuantificados contra el sistema real** corriendo (Docker Compose + MySQL 8 con los datos reales migrados en F10.x), no inferidos por lectura de código.

Dos de ellos rompen garantías declaradas como innegociables en el plan:
- se puede persistir un **asiento CONFIRMADO desbalanceado**;
- el **aislamiento multi-tenant no existe** en ninguna vía de acceso.

La contrapartida: el núcleo contable está bien construido. El alta directa de asientos rechaza correctamente los 4 ataques de desbalanceo, el gate de período cerrado **está bien escrito** (valida rol antes que flag), la firma JWT resiste `alg=none` y manipulación de payload, no hay inyección SQL en ninguna vía, y la disciplina `BigDecimal`/`DECIMAL` es completa. Los defectos no están en el diseño del motor: están en **caminos que esquivan las validaciones que sí existen**.

---

## Metodología

Auditoría adversarial en dos capas:

1. **7 auditorías de código en paralelo**, una por dimensión (integridad contable, multimoneda, autorización y períodos, IDOR/SQLi, JWT/secretos/logs, cobertura de auditoría, generadores vs. casos de prueba), con instrucción explícita de intentar romper las garantías y de citar `archivo:línea` para cada hallazgo.
2. **Verificación en vivo** contra el sistema real por parte del auditor principal. Ninguna acusación de BLOQUEANTE se publica en este informe sin reproducción, salvo las marcadas explícitamente como *no verificadas en vivo*.

**Todos los artefactos de prueba fueron limpiados**: asientos de prueba anulados, usuarios de auditoría desactivados, borradores eliminados. Verificación final de que la contabilidad real quedó intacta: **Σdebe = Σhaber = $305.621.023,15, diferencia $0,00**.

---

## Tabla de hallazgos

| # | Sev. | Hallazgo | Verificado en vivo |
|---|---|---|---|
| B1 | 🔴 BLOQ | Asiento CONFIRMADO desbalanceado vía `editarConfirmado` con `id` de línea repetido | ✅ Sí |
| B2 | 🔴 BLOQ | Aislamiento multi-tenant inexistente (lectura, escritura, listados y búsqueda) | ✅ Sí |
| B3 | 🔴 BLOQ | Toma de cuenta cruzada entre tenants vía reset de contraseña | ✅ Sí |
| B4 | 🔴 BLOQ | Log de auditoría sin filtro de tenant | ✅ Sí |
| B5 | 🔴 BLOQ | Escritura contable en período cerrado sin rol admin (`confirmar` + `registrarAutomatico`) | ✅ Sí |
| B6 | 🔴 BLOQ | Secreto de firma JWT versionado en git e idéntico al de producción | ✅ Documental |
| B7 | 🔴 BLOQ | Cobros/pagos en USD con 2+ componentes no se pueden confirmar (redondeo) | ❌ Prueba aritmética |
| B8 | 🔴 BLOQ | La base imponible de IIBB suma facturas en USD como si fueran pesos | ✅ Cuantificado sobre datos reales |
| A1 | 🟠 ALTO | Sin validación de rango del tipo de cambio: USD a TC=1, 0 o negativo | ✅ Sí |
| A2 | 🟠 ALTO | `tipoCambioEfectivo` de 6 decimales dispara `MONTO_ARS_INCONSISTENTE` en imputaciones > 10.000 USD | ❌ Prueba aritmética |
| A3 | 🟠 ALTO | Usuario desactivado conserva acceso indefinido vía `/auth/refresh` | ❌ Lectura de código |
| A4 | 🟠 ALTO | Perfil `dev` por defecto con secreto JWT hardcodeado | ✅ Documental |
| A5 | 🟠 ALTO | CBU, cuenta de tarjeta, CUITs y domicilio reales en fixtures de test versionadas | ✅ Documental |
| A6 | 🟠 ALTO | Job de alertas corre con el filtro de tenant deshabilitado | ❌ Lectura de código |
| A7 | 🟠 ALTO | Escritura cruzada de FKs: se referencian entidades de otro tenant | ❌ Lectura de código |
| A8 | 🟠 ALTO | `confirmarVinculandoAsientoExistente` (F10.3) sin gate de período | ❌ Lectura de código |
| A9 | 🟠 ALTO | Asiento vinculado por F10.3 anulable directo → documento queda inanulable | ❌ Lectura de código |
| A10 | 🟠 ALTO | Un asiento puede vincularse a N documentos (sin UNIQUE en `asiento_id`) | ❌ Lectura de código |
| A11 | 🟠 ALTO | `asientoIdExistente` arbitrario desde el endpoint público de importación | ❌ Lectura de código |
| A12 | 🟠 ALTO | 5 endpoints de configuración de admin escriben sin auditoría | ❌ Lectura de código |
| A13 | 🟠 ALTO | No se registra ningún intento de login fallido | ❌ Lectura de código |
| A14 | 🟠 ALTO | Asiento bajo override de período cerrado se audita como escritura normal | ❌ Lectura de código |
| A15 | 🟠 ALTO | Retención de IIBB/SIRCREB sufrida en un cobro no se puede registrar (impacto matizado — ver detalle) | ❌ Lectura de código |
| A16 | 🟠 ALTO | `confirmarVinculandoAsientoExistente` (F10.3) deja `montoArsCancelado` en NULL → NPE (**latente**) | ✅ Sí — verificado NO materializado |
| A17 | 🟠 ALTO | IIBB: nada valida que la suma de coeficientes de Convenio Multilateral dé 1 | ❌ Lectura de código |
| A18 | 🟠 ALTO | IIBB: la herencia del coeficiente del mes anterior mezcla dos criterios incompatibles | ❌ Lectura de código |
| A19 | 🟠 ALTO | IIBB: ventas a jurisdicción inactiva engordan la base y no tributan, sin advertencia | ❌ Lectura de código |

Además **18 hallazgos MEDIO** y **14 BAJO** detallados en la sección correspondiente.

---

## BLOQUEANTES

### B1 — Asiento CONFIRMADO desbalanceado vía línea duplicada 🔴

**Rompe la regla innegociable central del sistema.**

- **Dónde**: `contabilidad/asiento/AsientoService.java:241-272`
- **Qué pasa**: el bucle que reconstruye las líneas resuelve `existentesPorId.get(r.id())` y hace `add(...)` **sin deduplicar por `id`**. Si el pedido repite un `id`, la misma instancia entra dos veces en la lista en memoria. La validación de balance corre sobre esa lista (contando la línea dos veces, por lo que "cuadra"), pero la asociación es un bag inverso `@OneToMany(mappedBy=...)`: Hibernate persiste **una sola fila**. La validación ve N+1 líneas y la base guarda N.
- **Repro verificado en vivo**:
  ```
  # asiento CONFIRMADO con línea 1946 (debe 1000) y línea 1947 (haber 1000)
  PUT /api/v1/asientos/850/confirmado
  { "lineas": [ {"id":1946, "debe":1000, ...},
                {"id":1946, "debe":1000, ...},     ← id repetido
                {"id":1947, "haber":2000, ...} ] }
  → HTTP 200, la API responde totalDebe=2000, totalHaber=2000
  ```
  Lo que quedó realmente en la base:
  ```
  id=850  numero=822  estado=CONFIRMADO
  debe_real=1000.00   haber_real=2000.00   DESCUADRE = -1000.00
  ```
- **Por qué importa**: el descuadre entra directo al Mayor, al Balance de sumas y saldos y al Estado de Resultados —los tres leen `asiento_linea` filtrando por `estado=CONFIRMADO` y nunca revalidan balance—. No hay CHECK en base ni job de verificación. **Y la API informa totales cuadrados que la base no tiene**, así que es indetectable por pantalla. Ejecutable con rol CARGA.
- **Fix**: rechazar ids repetidos al inicio de `editarConfirmado`; validar sobre `LinkedHashSet` (identidad de instancia) como defensa en profundidad; y agregar un test de integración que relea el asiento desde la base (`entityManager.clear()`) y asserte `Σdebe == Σhaber` — los tests actuales validan contra el objeto en memoria, que es exactamente lo que deja pasar el bug.

---

### B2 — Aislamiento multi-tenant inexistente 🔴

- **Causa raíz**: el aislamiento se apoya en **un único mecanismo**, el filtro de Hibernate `tenantFilter` declarado en `common/tenant/EntidadNegocio.java:35-36`. Hibernate **no aplica los `@Filter` a la carga por identificador** (`EntityManager#find`, que es lo que hace `findById`) ni a la resolución de `@ManyToOne`. Y todo el sistema lee y escribe por `findById` (137 llamadas en 47 servicios). No existe ni una sola comparación de `getTenantId()` contra `TenantContext` en todo `src/main/java`.
- **Repro verificado en vivo**, con el token del admin del *Tenant Test* (tenant 2) contra los datos reales de Montanari Tech (tenant 1):

  | Vía | Resultado |
  |---|---|
  | `GET /clientes` | devuelve los **19 clientes** del tenant 1, con CUIT |
  | `GET /facturas-venta` | devuelve las **60 facturas** reales |
  | `GET /asientos/186` | devuelve el **asiento de apertura** completo |
  | `GET /busqueda?q=TALLER` | devuelve facturas reales del tenant 1 |
  | `PUT /cuentas-bancarias/1` | **HTTP 200 — escritura cruzada aceptada** |

  Verificado también en sentido inverso (tenant 1 → tenant 2).
- **Nota**: los listados que van por JPQL *sí* deberían estar filtrados por el mecanismo, pero en la práctica devolvieron datos cruzados — el diagnóstico fino de por qué (interacción con `open-in-view` y el momento de habilitación del filtro) queda para el fix. El `id` es autoincremental **global**, así que enumerar es trivial.
- **Fix**: no confiar en el filtro para acceso por id. Un `BaseRepository` con `findByIdAndTenantId(...)` (que sí pasa por JPQL) + un guard central que tire 404 si el tenant no coincide, prohibiendo `findById` con ArchUnit. Y agregar un test de integración de dos tenants — hoy **no existe ninguno**.

---

### B3 — Toma de cuenta cruzada entre tenants 🔴

Escalada de B2 a compromiso total.

- **Dónde**: `auth/UsuarioService.java:86-92`, vía `obtener(id)` en `:47-49`
- **Repro verificado en vivo**:
  ```
  # token del ADMIN DEL TENANT 2
  GET /api/v1/usuarios/4          → 200, devuelve email y rol de un usuario del TENANT 1
  PUT /api/v1/usuarios/4/password  {"passwordNueva":"TomadoPorT2!"}   → 204
  # y después, sin ningún token:
  POST /api/v1/auth/login  {"email":"<usuario del tenant 1>","password":"TomadoPorT2!"}  → 200
  ```
- **Por qué importa**: cualquier administrador de un tenant se apodera de cuentas de todos los demás. `editar()` permite además cambiarles el rol.
- **Fix**: validación explícita de tenant en `UsuarioService` antes de cualquier mutación, además del guard general de B2.

---

### B4 — Log de auditoría sin filtro de tenant 🔴

- **Dónde**: `common/audit/AuditoriaLog.java:35-36` (no extiende `EntidadNegocio`, así que el `@Filter` nunca se le aplica) + `common/audit/AuditoriaLogRepository.java:12-26` (JPQL sin predicado de tenant)
- **Repro verificado en vivo**: `GET /api/v1/auditoria?size=200` con token del tenant 2 devuelve **`totalElements: 2738`**, incluyendo entradas de `Asiento` (73), `MovimientoBancario` (57), `Cobro` (22), `FacturaCompra` (12), `Pago` (12) del tenant 1.
- **Por qué importa**: no es metadata. `AuditoriaLogResponse` expone `datosAntes`/`datosDespues`, que son los **snapshots JSON completos** de cada entidad antes y después de cada operación. Es exfiltración masiva de la contabilidad ajena con un solo GET, sin necesidad de adivinar ids. Los índices de la migración arrancan con `tenant_id`, o sea que el diseño de datos asumía este filtro y la capa Java nunca lo aplicó.
- **Hallazgo relacionado (MEDIO)**: verificado que una escritura cruzada queda archivada bajo el `tenant_id` del **atacante**, no del dueño del dato — un auditor del tenant víctima no vería la modificación de su propia cuenta bancaria.
- **Fix**: `AND a.tenantId = :tenantId` en la query, alimentado desde `TenantContext`.

---

### B5 — Escritura contable en período cerrado sin rol admin 🔴

**El gate está bien escrito; el problema es que seis vías nunca lo invocan.**

Se verificó explícitamente que `PeriodoService.verificarEscritura` (`periodo/PeriodoService.java:66-79`) **no es vulnerable al flag booleano**: chequea `esAdmin()` *antes* de mirar `confirmarPeriodoCerrado`, así que un usuario CARGA que manda `confirmarPeriodoCerrado=true` es correctamente rechazado (probado en vivo → 422).

El defecto real es la cobertura:

- **`AsientoService.confirmar` (`:187-200`) no verifica el período.** Su propio Javadoc lo admite: *"el ítem 5 —PeriodoGuard— no aplica todavía"*. `crearBorrador` sí está gateado, pero confirmar —que es el momento en que el asiento entra a los libros— no.
  **Repro verificado en vivo**: un borrador con fecha 15/01/2025 (período CERRADO) fue confirmado por un usuario **rol CARGA sin override** → HTTP 200, número 821 asignado, estado CONFIRMADO.
- **`AsientoService.registrarAutomatico` (`:405-443`) tampoco.** Es "el único punto de entrada de los generadores automáticos", y cuatro servicios lo llaman sin gate previo: `MovimientoBancarioService` (`:233`, alcanzado desde `confirmar` e `imputar`), `PagoTarjetaService` (`:88`), `LiquidacionIvaService` (`:231`) y `LiquidacionIibbService` (`:281`). En IVA/IIBB `PeriodoService` está inyectado pero se usa **solo para un texto de advertencia**, nunca para bloquear.
- **Agravante de auditoría**: ambos caminos auditan con la sobrecarga *sin* `sobrePeriodoCerrado`, así que estos asientos quedan con `sobre_periodo_cerrado = false`. Un auditor que filtre por ese flag para listar escrituras sobre ejercicios cerrados **no los va a encontrar**.
- **Fix**: mover el gate al propio `registrarAutomatico` (cierra los 4 agujeros de una vez) y agregar la sobrecarga con verificación a `confirmar`, exponiendo los dos `@RequestParam` en el controller como ya hacen `crear`/`editar`/`anular`.

---

### B6 — Secreto de firma JWT versionado en git 🔴

- **Dónde**: `.env.example:12` (`JWT_SECRET=...`, 64 chars de alta entropía), commiteado desde `b61e04a` (F1.4/F1.5/F1.6)
- **Verificación documental**: `diff .env .env.example` → **byte-idénticos**. El `README.md:19` documenta `cp .env.example .env`, que es lo que garantiza esa igualdad. El comentario del propio archivo ("Rotar antes de cualquier uso real") no se siguió.
- **Por qué importa**: con ese secreto se firma un HS256 con `{"sub":"1","rol":"ADMINISTRADOR","tenantId":<cualquiera>}` y `JwtAuthenticationFilter` lo acepta sin consultar la base. Acceso administrativo a cualquier tenant, sin credenciales y sin explotar ninguna otra vulnerabilidad. Cualquiera con acceso al repo, a un clon, a un fork o al historial.
- **Fix**: rotar el secreto ya (invalida los tokens vigentes, que es lo deseado); reemplazar el valor por un placeholder inerte; considerar el valor quemado para siempre o purgar el historial; agregar un secret-scanner al CI.

---

### B7 — Cobros y pagos en USD con 2+ componentes no se pueden confirmar 🔴

*No verificado en vivo (requiere montar facturas USD específicas); sustentado en prueba aritmética del código.*

- **Dónde**: `facturacion/cobro/CobroAsientoGenerator.java:94` (y `:157`, `:172`) vs `:139`; simétrico en `PagoAsientoGenerator.java`
- **Qué pasa**: la línea de fondos redondea **una vez sobre el agregado** (`round2(total × tc)`) mientras el otro lado redondea **componente por componente**. Algebraicamente `Σ round2(xᵢ × tc) ≠ round2((Σxᵢ) × tc)` cuando hay más de un componente, y `ValidadorBalanceAsiento` rechaza el asiento.
- **Ejemplo**: cobro USD 1.819,39 @ TC 1.240,18 imputado a dos facturas (USD 712,75 y 1.106,64) → Debe 2.256.371,09 vs Haber 2.256.371,10 → delta $0,01 → no se puede confirmar. Un barrido de 2.000 cobros simulados con 2–4 imputaciones y TC de 2 decimales: **31,6 % no balancean**.
- **Falla cerrado** (no persiste números mal), pero bloquea un flujo central sin mensaje accionable. Se dispara con varias imputaciones, o imputación + anticipo, o + retenciones, o + recargo de mora. En ARS nunca ocurre porque `tc=1`.
- **Consistente con lo observado en F10.3**: todos los cobros USD que se crearon en la reconstrucción tenían **una sola** imputación, que es justo el régimen donde el bug desaparece.
- **Fix**: aplicar la regla del residuo también en la conversión a ARS — anclar en el agregado y derivar el último componente por diferencia.

---

## ALTOS destacados

**A1 — Sin validación de rango del tipo de cambio (verificado en vivo).** Los 6 DTOs con `tipoCambio` llevan solo `@NotNull`; no hay un solo `@Positive` ni `@DecimalMin` sobre un TC en todo el backend, y solo `asiento_linea` tiene `CHECK` a nivel de esquema. Verificado en vivo sobre facturas de venta en USD:

| TC enviado | Resultado |
|---|---|
| `1` | ✅ aceptado → USD 1.000 persistido como **ARS 1.000** (subvaluado ~1.400×) |
| `0` | ✅ aceptado → `totalArs = 0.00` |
| `-350` | ✅ aceptado → `totalArs = -350.000,00` |

Es la falla silenciosa más peligrosa del circuito multimoneda: el registro queda *internamente consistente*, así que el generador balancea y `MONTO_ARS_INCONSISTENTE` no objeta nada. El error se propaga a CxC, balance, estado de resultados e IVA sin que ninguna validación lo frene. **Fix**: `@DecimalMin` en los 6 DTOs + `CHECK (tipo_cambio > 0)` por migración en las 7 tablas que no lo tienen.

**A3 — Usuario desactivado conserva acceso indefinido.** `RefreshTokenService.consumirYRotar` (`:49-56`) valida hash, revocación y vencimiento pero **nunca consulta `usuario.isActivo()`**; el chequeo solo existe en el login por password. Como cada refresh emite un token nuevo a 7 días, la cadena se autorrenueva para siempre. Desactivar un usuario —el mecanismo de baja del sistema, no hay hard-delete— no lo saca. **Fix**: filtrar por `isActivo` y revocar en bloque los refresh tokens en `desactivar`/`editar`/`cambiarPassword`.

**A5 — PII y datos bancarios reales en fixtures versionadas.** `.gitignore` excluye correctamente `facturasyresumenes/`, pero el **contenido extraído** de esos PDFs se copió literal a tests: CBU real y número de cuenta en `ParserGaliciaTest.java:124`, resumen VISA completo con domicilio fiscal y número de tarjeta en `ParserTarjetaTest.java:22-60`, y CUITs con domicilio particular de personas físicas en `ExtractorFacturaPdfTest.java`. La protección se aplicó al contenedor, no al dato. Riesgo bajo Ley 25.326.

**A12/A13/A14 — Brechas de auditoría.** Cinco endpoints `PUT` de configuración de administrador escriben sin ningún registro (el más grave: `ConfiguracionPresupuestoController`, con 15 alícuotas incluida IVA e IIBB; y `CobroController.actualizarConfiguracionCobranza`, que fija la tasa de mora que impacta contabilidad real). No se registra **ningún intento de login fallido**, ni logout. Y el asiento generado bajo override de período cerrado se audita como escritura normal (ver B5).

---

## Generadores de asientos vs. casos de prueba (F3.1 / F4.1)

**Los 15 casos numéricos de F4.1 §7 reproducen al centavo.** Se recalculó cada uno contra el código (FV-1..3, FC-1..4, CO-1..5, PA-1..3) sin encontrar ninguna divergencia aritmética. También verificados sin hallazgos: balance sin tolerancia (§0.1), `generada_auto = true` (§0.5), resolución específico→default→error de `ResolutorCuentas` (§1.2), el catálogo de 14 `ConceptoContable` (§1.3), y la inversión de lados en notas de crédito.

Los defectos están en los bordes:

**A15 — La retención de IIBB/SIRCREB sufrida en un cobro no se puede registrar.** La spec (F4.1 §6.1) define **tres** tipos de retención sufrida con su cuenta: Ganancias → `1.1.2011`, IVA → `1.1.2007`, **IIBB/SIRCREB → `1.1.2008`**. El código mapea solo dos: `CobroAsientoGenerator.java:74-79` tira `TRIBUTO_NO_APLICABLE_A_COBRO` para todo lo demás, y `CobroService.java:45-47` lo blinda antes con un `Set.of(RETENCION_GANANCIAS, RETENCION_IVA)`. No existe `RETENCION_IIBB_SUFRIDA` en `ConceptoContable` — aunque `TipoTributo` **sí** declara `RETENCION_IIBB` y `SIRCREB`, y el DTO los acepta sintácticamente.
**Impacto aguas abajo**: `CalculoIibbService.java:132` toma las deducciones de IIBB de la cuenta `1.1.2008`. Una retención que nunca puede imputarse ahí es una deducción que la liquidación **no computa** → *se paga IIBB de más*. SIRCREB está explícitamente en el alcance del proyecto y aparece en los resúmenes bancarios reales. No hay nada en `outputs/` que documente la omisión como decisión.

**A16 — `confirmarVinculandoAsientoExistente` deja `montoArsCancelado` en NULL (latente, no materializado).** El camino agregado en F10.3 Fase B saltea el generador y va directo a `setEstado(CONFIRMADO)`, sin ejecutar `imputacion.setMontoArsCancelado(...)` ni `cobro.setMontoAnticipo(...)`. La columna es nullable, así que persiste el NULL; después `CobroService:286` y `CobroAsientoGenerator:120-122` hacen `reduce(ZERO, BigDecimal::add)` sobre ese valor → **NullPointerException**, y la factura queda imposible de cancelar. Los tests no lo ven porque `CobroServiceTest.java:159-176` construye el cobro **sin imputaciones**, así que el `reduce` nunca corre.
**Verificado en vivo que NO afectó los datos reales**: `cobro_imputacion` y `pago_imputacion` tienen **0 filas con `monto_ars_cancelado` NULL** sobre 45 y 41 respectivamente. La reconstrucción de F10.3 Fase D usó el camino normal `confirmar` (que sí corre el generador), no el vinculado. Es un arma cargada, no una herida abierta — pero el endpoint existe y F10.3 dejó el patrón instalado.

**Tres brechas de test que explican por qué varios bugs sobrevivieron** (MEDIO):
- **La regla del residuo nunca se ejercita con un residuo real.** La implementación de `CalculoImputacion.java:35-41` es correcta, pero **no existe `CalculoImputacionTest`** y todos los TC de los tests son enteros — con TC entero, `round2(x × TC)` es exacto y la rama del residuo devuelve el mismo número que la fórmula. Si se borrara la rama entera, los tests seguirían en verde. (Consecuencia: la afirmación de `outputs/F4_4_cobros_y_pagos.md:12` de que la regla fue "verificada en vivo con CO-3" es vacua — ese caso no produce residuo.)
- **El ajuste de aplicación de anticipo del lado Pago nunca se ejecuta**: `PagoServiceTest.java:237` solo lo mockea. Es el único método de los generadores que mueve dinero y jamás corre bajo test, y su signo es el inverso del lado Cobro — justo el error más fácil de introducir.
- **`computaCreditoFiscal` solo excluye `FACTURA_C`**: `FacturaCompraAsientoGenerator.java:120-125`. De los 15 valores de `TipoComprobante`, solo uno bloquea el crédito fiscal, así que un `TICKET`, `RECIBO` o `FACTURA_B` de un proveedor RI computa IVA crédito fiscal. Fiscalmente una Factura B no discrimina IVA y un recibo no habilita cómputo. La documentación de F4.3 razonó sobre dos condiciones y nunca consideró esos tipos: es omisión, no decisión.

**Divergencia justificada (no es hallazgo)**: la ausencia de fila por defecto en `mapeo_cuenta` para `CREDITO_POR_VENTA`/`DEUDA_COMERCIAL`/`COSTO_GASTO`, que la spec §2.1 pedía. Las migraciones la omiten deliberadamente y con constancia escrita (`V20:47`, `V21:23-25`): fallar fuerte con `MAPEO_CUENTA_FALTANTE` es contablemente más correcto que volcar en silencio los clientes nuevos a una cuenta genérica. Consecuencia operativa a tener presente: un cliente o proveedor nuevo no puede confirmar su primera factura hasta que un admin le asigne la cuenta.

---

## Liquidaciones de impuestos (F6.1 IVA / F6.2 IIBB)

*Nota de proceso: esta área requirió una segunda pasada. El primer auditor afirmó haberla cubierto y luego se autocorrigió reconociendo que no lo había hecho; se lanzó una auditoría dedicada, cuyos resultados son estos.*

### IVA (F6.1) — sin hallazgos en el núcleo

Los 12 escenarios E2E de §2.3 y las 4 correcciones del checkpoint de §3.5 **están todos implementados y con test**. Se verificaron específicamente los cuatro puntos más frágiles según la propia spec, y los cuatro son correctos:

- **Art. 24 (saldo técnico vs. libre disponibilidad)**: el excedente técnico **no** puede consumirse contra ingresos directos — `ResultadoIva:40` usa `tecnica.max(ZERO)` como impuesto determinado. Las dos especies conviven, arrastran por separado y van a cuentas distintas (1.1.2014 / 1.1.2015).
- **Art. 11/12 (notas de crédito por lado de imputación)**: la NC emitida se lee del **debe** de 2.1.2008 y aporta a la etapa técnica sin bajar el débito fiscal. Correcto.
- **Exclusión de `origen = LIQUIDACION_IVA`**: está en la query y se aplica a todos los componentes automáticos — el impuesto no se realimenta.
- **Arrastre entre períodos**: correcto para ambas especies, incluido el salto enero→diciembre del año anterior.

El balance del asiento es exacto por álgebra y toda la aritmética corre en escala 2, sin deriva de redondeo. **El "bug de moneda" que la memoria del proyecto menciona no aplica a IVA**, porque lee `AsientoLinea.debe/haber`, que ya están en ARS.

Hallazgos menores de IVA: al confirmar no se revalida el cálculo (MEDIO — una liquidación creada antes de que se confirmaran facturas del período se declara con números obsoletos, y el asiento balancea igual, así que nada lo detecta); la advertencia de "sin liquidación anterior" se genera pero `crearBorrador` la descarta (BAJO); y la calibración de junio 2026 contra la hoja del contador **no quedó como test de regresión** (BAJO — son 6 cifras y 10 líneas de test que protegen el único punto que el contador firmó).

### B8 — La base imponible de IIBB suma facturas en USD como si fueran pesos 🔴

- **Dónde**: `impuestos/iibb/CalculoIibbService.java:63` — `BigDecimal neto = f.getNetoGravado();`, sumado directamente a `baseTotal` en `:67`.
- **Qué pasa**: `FacturaVenta.netoGravado` está **en la moneda del comprobante**, no en pesos (`FacturaVentaService.recalcularTotales` lo arma sumando importes en moneda original y materializa `totalArs` aparte). `CalculoIibbService` es el único consumidor que suma `netoGravado` **sin multiplicar por `tipoCambio`**, y ni siquiera lee `getMoneda()`. Es el mismo patrón del bug de moneda ya conocido en el proyecto: el helper de test nunca setea moneda ni TC, así que los 6 tests son implícitamente ARS y el defecto no puede aparecer.
- **Cuantificado sobre los datos reales migrados** (6 períodos con facturas en USD):

  | Período | Base que usa el sistema | Base correcta en ARS | Base no declarada |
  |---|---:|---:|---:|
  | 2025-11 | 174.264,51 | 864.470,76 | **690.206,25** |
  | 2025-12 | 2.169.340,02 | 3.624.674,52 | **1.455.334,50** |
  | 2026-01 | 3.841.744,37 | 4.543.060,37 | **701.316,00** |
  | 2026-02 | 9.135.086,59 | 10.494.490,09 | **1.359.403,50** |
  | 2026-03 | 9.486.951,77 | 10.162.668,77 | **675.717,00** |
  | 2026-04 | 531.045,94 | 1.186.476,94 | **655.431,00** |

  Total: **~$5,5 M de base imponible mal computada** en 6 meses.
- **Matiz importante para el contador — el fix no es solo multiplicar por el TC**: de las 9 facturas en USD, **8 son FACTURA_E de exportación** (Jarp Inc, Puerto Rico), que en la mayoría de las jurisdicciones **no están gravadas por IIBB** y probablemente no deberían integrar la base en absoluto. Pero **1 es una FACTURA_A en USD a un cliente local** (USD 725), que es una venta gravada nominada en moneda extranjera y hoy se declara como **$725 en vez de ~$1.050.000**.
  O sea: el código actual no hace ninguna de las dos cosas correctas — no excluye las exportaciones ni convierte las ventas locales en USD. Además, incluir las exportaciones a 1/1400 de su valor **distorsiona el denominador de todos los coeficientes por defecto**, contaminando el reparto entre jurisdicciones.
- **Fix**: convertir con `f.getNetoGravado().multiply(f.getTipoCambio())` (el TC vale exactamente 1,000000 en ARS, así que no cambia nada en pesos) **y** decidir con el contador el tratamiento de las facturas de exportación — probablemente excluirlas de la base. Agregar un test con una factura USD y TC ≠ 1.

### Otros hallazgos de IIBB

**A17 — Nada valida que la suma de coeficientes de Convenio Multilateral dé 1.** `@DecimalMin("0.0")` bloquea negativos pero no hay `@DecimalMax` ni ninguna comprobación de Σ, ni en el backend ni en el frontend. Un typo de 0,700000 en vez de 0,300000 (Σ = 1,4) sobre-declara el 48% de la base y se puede confirmar sin una sola advertencia; el caso simétrico sub-declara. Un coeficiente de 5,0 también se acepta.

**A18 — La herencia del coeficiente del mes anterior mezcla dos criterios incompatibles.** El fallback opera *por jurisdicción*, no *por liquidación*: si aparece una jurisdicción nueva, hereda las demás el coeficiente CM real y la nueva cae al criterio "participación por destino". Los dos criterios no son conmensurables y su suma no da 1 por construcción — el ejemplo verificado da Σ = 1,1 (10% de base de más) sin advertencia. Sin test que cubra la herencia.

**A19 — Ventas a una jurisdicción inactiva engordan la base y no tributan.** `baseTotal` suma todas las facturas, pero el reparto solo itera jurisdicciones **activas**, y la advertencia de "sin jurisdicción" solo cubre el caso `null`. Una venta a una jurisdicción existente pero desactivada entra a la base, no genera sub-liquidación, no dispara advertencia, y de paso baja los coeficientes por defecto de todas las demás.

**A15 (corregido) — Retención de IIBB/SIRCREB sufrida en un cobro.** Corrijo el alcance respecto de lo reportado en la sección de generadores: las **otras dos vías a la cuenta 1.1.2008 sí funcionan** (percepción en factura de compra, y clasificación de SIRCREB desde la conciliación bancaria). El agujero es específicamente la **retención practicada por el cliente al cobrar**, que ni siquiera permite confirmar el cobro. Y el impacto no es necesariamente "pagar de más": las deducciones son manuales por diseño, así que si el usuario la carga a mano la liquidación sale bien. Lo que sí queda roto es la **contabilidad**: el asiento de la liquidación acredita 1.1.2008 contra un activo que nunca se debitó, dejando un **pasivo ficticio** en el balance. El fix es una línea: agregar `RETENCION_IIBB` y `SIRCREB` al `switch` de `CobroAsientoGenerator`, reusando el mapeo que ya existe.

**MEDIO** — `recalcular` de IIBB no refresca el "traer de contabilidad": si se concilia el SIRCREB después de abrir el borrador, la deducción se pierde y se paga de más. **BAJO** — no hay regla del residuo en el prorrateo entre jurisdicciones (a diferencia de F6.3, que sí la tiene), así que Σ bases puede no dar el total exacto.

---

## Lo que resistió

Vale registrarlo con el mismo rigor que los defectos:

- **Balance en el alta directa**: 4 ataques probados en vivo (desbalance grosero, descuadre de $0,01, una sola línea, debe+haber en la misma línea) → los 4 rechazados con 422 y mensaje correcto.
- **Inyección SQL**: **sin hallazgos**. Las 3 queries nativas del proyecto (FULLTEXT de F9.2) usan parámetros bindeados y llevan `tenant_id` explícito; cero `createNativeQuery`/`createQuery`; ninguna concatenación en `@Query`; el ordenamiento no acepta nombres de columna del cliente.
- **Firma JWT**: `alg=none`, payload manipulado y firma vacía → 401 en los tres casos. jjwt 0.12.6 con `verifyWith` no es vulnerable a confusión de algoritmo. El refresh es opaco y se guarda hasheado (SHA-256). Contraseñas con BCrypt.
- **Rol LECTURA**: no puede escribir (403 verificado en `/asientos` y `/clientes`).
- **Gate de período cerrado**: bien construido — valida rol antes que flag (ver B5, el defecto es de cobertura, no de diseño).
- **`float`/`double` en importes**: limpio. Las 6 apariciones en el backend son geometría de PDF y cálculo de paginación. Cero columnas `DOUBLE`/`FLOAT` en las 46 migraciones.
- **Diferencia de cambio**: signo correcto en las 4 direcciones, contrastado caso por caso contra la spec de F4.1 §7 (CO-2, CO-3, PA-1, PA-3 y el ajuste CO-5).
- **Punto único de escritura contable (ADR-07)**: verificado. No hay ningún `save()`/`@Modifying` sobre `Asiento`/`AsientoLinea` fuera de `AsientoService`.
- **Numeración de asientos**: correcta. `SELECT ... FOR UPDATE` sobre la secuencia, número asignado *después* de validar (un fallo no consume número), UNIQUE real `(tenant_id, numero)` en la base.
- **Cobertura de auditoría en el núcleo**: los 21 servicios de maestros son simétricos (crear/editar/estado/eliminar); liquidaciones de IVA e IIBB completas con antes/después reales.
- **Manejo de logs**: cero `System.out`/`printStackTrace` en todo el backend; ningún stacktrace llega al cliente; el login no distingue usuario inexistente de contraseña errónea.

---

## Recomendación para el checkpoint humano

**No hacer go-live** hasta resolver B1–B6. Orden sugerido por costo/impacto:

1. **B6** (rotar el secreto JWT) — minutos, y sin él todo lo demás es irrelevante.
2. **B1** (deduplicar ids en `editarConfirmado`) — pocas líneas, y es la única regla innegociable del sistema.
3. **B5** (gate en `confirmar` y `registrarAutomatico`) — acotado, un solo lugar cierra 4 de las 6 vías.
4. **A1** (validación de rango del TC) — anotaciones + una migración; previene corrupción silenciosa de datos.
5. **B2/B3/B4** (aislamiento multi-tenant) — es el más caro porque es sistémico (137 call sites). **Decisión de negocio a tomar**: si el go-live es mono-empresa, el riesgo real hoy es acotado (existe un solo tenant productivo) y podría diferirse con una mitigación temporal —por ejemplo, no crear un segundo tenant hasta cerrarlo—; pero entonces conviene documentarlo como deuda bloqueante para cualquier escenario multiempresa, no como resuelto.
6. **B7/A2** (redondeo multimoneda) — bloquean operación real en USD; necesarios antes de operar cobros en moneda extranjera con más de una imputación.
7. **B8 + A17/A18/A19** (base y coeficientes de IIBB) — **requiere una decisión del contador, no solo un fix de código**: cómo tratar las facturas de exportación en la base de IIBB. Con ~$5,5 M de base mal computada en 6 meses de datos ya migrados, cualquier liquidación de IIBB emitida hoy sobre este período saldría mal.
8. **A15** (retención IIBB/SIRCREB no registrable) — una línea de código; hoy impide confirmar un cobro con retención de IIBB y deja un pasivo ficticio en 1.1.2008.

### Sobre los datos ya migrados en F10.x

La reconstrucción histórica está **contablemente sana** (balance verificado, Σdebe = Σhaber), pero conviene tener presente que:
- **No se emitió ninguna liquidación de IIBB** sobre los períodos migrados. Si se emite antes de arreglar B8, saldrá con la base mal calculada.
- El bug A16 (`montoArsCancelado` NULL) **no afectó** los datos reales — verificado, 0 filas nulas.
- La factura en USD a cliente local que dispara B8 es real y está confirmada en el sistema.

### Nota transversal sobre los tests

Hay un patrón que atraviesa la mayoría de los hallazgos serios: **los tests validan el régimen donde el bug no aparece**. Objetos en memoria en vez de releer de la base (B1); importes chicos y una sola imputación (B7, A2); helpers de test que nunca setean moneda ni tipo de cambio (B8); mocks en lugar de ejecución real (ajuste de anticipo del lado Pago); tipos de cambio enteros que hacen exacta la regla del residuo; y **cero tests de aislamiento entre dos tenants** (B2/B3/B4). Los fixes deberían venir con tests en esos regímenes, o los bugs vuelven.

**Nota sobre los tests**: hay un patrón que atraviesa varios hallazgos — los tests unitarios validan contra objetos en memoria (B1), usan importes chicos y una sola imputación (B7, A2), y no existe ningún test de aislamiento entre dos tenants (B2). Los fixes deberían venir acompañados de tests en esos tres regímenes, o los bugs vuelven.

---

## Verificación

- **Sistema auditado**: Docker Compose + MySQL 8 con los datos reales migrados en F10.1/F10.2/F10.3 (816 asientos confirmados, 60 facturas de venta, 108 de compra, 491 movimientos bancarios).
- **Alcance del código**: 71 `@RestController` (223 endpoints de escritura), 46 migraciones Flyway, los 7 `*AsientoGenerator`, los 9 importadores, y las specs de F3.1/F4.1/F6.1/F6.2 — las cuatro cubiertas.
- **Límites declarados de la auditoría**: no se ejecutó la suite de tests como parte de la revisión de IVA/IIBB (los conteos de cobertura salen de leer los tests, no de correrlos); no se verificaron las migraciones V27/V28/V29 contra los mapeos concepto→cuenta que asume `ResolutorCuentas`; y la revisión de frontend se limitó a la validación de coeficientes de IIBB. Los 12+4 escenarios E2E de IVA se auditaron sobre la lógica que los produce, no reproduciéndolos numéricamente contra una base nueva.
- **Limpieza post-auditoría verificada**: 0 asientos de prueba en estado CONFIRMADO, usuarios de auditoría desactivados, y balance real intacto (**Σdebe = Σhaber = $305.621.023,15, diferencia $0,00**).
- **Sin cambios de código en este paso**: F11.1 es un paso de revisión. Los fixes corresponden a F11.2 o a un paso de remediación que el equipo defina en el checkpoint.
