# Guía de pruebas manuales + estado real del sistema

Documento de trabajo para arrancar las pruebas manuales locales. Tres partes:

1. [Cómo arrancar y con qué datos](#1-cómo-arrancar) — credenciales, URLs, qué hay cargado.
2. [Mapa de conexiones](#2-mapa-de-conexiones) — qué está conectado a qué, y qué **no** está conectado a nada.
3. [Plan de pruebas manuales](#3-plan-de-pruebas-manuales) — 8 sesiones ordenadas por dependencia.
4. [Pendientes](#4-pendientes) — de conectar, de desarrollar, credenciales, y lo que corresponde al VPS.

---

## 1. Cómo arrancar

### El stack ya está corriendo

Verificado al momento de escribir esto: los 3 contenedores arriba y sanos.

| Servicio | URL / puerto | Estado |
|---|---|---|
| Frontend (React) | http://localhost:5173 | 200 OK |
| Backend (Spring Boot) | http://localhost:8081 | `/actuator/health` → `UP` |
| Swagger UI (API) | http://localhost:8081/swagger-ui.html | — |
| MySQL 8 | `localhost:3307` | healthy |

Si hiciera falta levantarlo de cero:

```bash
docker compose up -d
```

### Credenciales

| Qué | Valor | Nota |
|---|---|---|
| Usuario admin | `admin@montanaritech.com` | Sembrado por Flyway |
| Contraseña | `changeme123` | **Rotar antes de cualquier uso real** — está en el README y en el repo |
| Base de datos | usuario/clave del `.env` local | El `.env` no está versionado |

Los roles del sistema son tres: **ADMINISTRADOR**, **CARGA**, **LECTURA**. El admin sembrado es ADMINISTRADOR. Para probar los permisos por rol hay que crear usuarios de prueba desde **Seguridad** (`/seguridad`).

### Qué datos hay cargados (reales, no de prueba)

La base tiene la **contabilidad real reconstruida** de F10.2/F10.3 — no son fixtures:

| Entidad | Cantidad |
|---|---|
| Asientos contables | 847 |
| Líneas de asiento | 1.924 |
| Clientes | 19 |
| Proveedores | 13 |
| Facturas de venta | 60 |
| Facturas de compra | 108 |
| Movimientos bancarios | 492 |

**Invariante de control:** `Σ debe = Σ haber = $305.621.023,15`, diferencia $0,00. Cada vez que termines una sesión de pruebas, verificá que esto siga cerrando (Balance de sumas y saldos → tiene que decir que balancea). Si en algún momento deja de cerrar, algo que hiciste rompió la partida doble y hay que investigarlo.

**Cobertura temporal de los datos:** octubre/2025 → abril/2026. Mayo, junio y julio de 2026 **no tienen documentos cargados** (no había archivos del cliente para esos meses). Tenelo en cuenta al probar reportes: si filtrás por un mes de ese rango vacío, es esperable que no aparezca nada.

### ⚠️ Antes de empezar: hacé un backup

Las pruebas incluyen confirmar, anular y cerrar períodos sobre datos reales. **Sacá un backup antes de tocar nada**, así podés volver atrás sin perder la reconstrucción histórica:

```bash
BACKUP_DIR=/c/temp/backup-pre-pruebas COMPOSE_FILE=./docker-compose.yml ops/backup/backup.sh
```

Para restaurar si algo sale mal (ver [RUNBOOK.md](RUNBOOK.md) § 7):

```bash
docker compose stop backend
ops/backup/restore.sh /c/temp/backup-pre-pruebas/montanari_contable_AAAAMMDD_HHMMSS.sql.gz --compose-service mysql
docker compose start backend
```

---

## 2. Mapa de conexiones

### 2.1 Conexiones internas (todo lo que hay)

```
   Navegador
      │  http://localhost:5173
      ▼
[ frontend ]  React 19 + Vite, servido por nginx en el contenedor
      │  llama a http://localhost:8081/api/...  (JWT en el header Authorization)
      ▼
[ backend ]   Spring Boot, puerto 8081 en el host / 8080 dentro del contenedor
      │  JDBC → mysql:3306 (red interna de Docker Compose)
      ▼
[ mysql ]     MySQL 8 — volumen `mysql_data` (los datos sobreviven a reinicios)
```

Detalle de cómo se conecta cada capa:

| Conexión | Mecanismo | Dónde se configura |
|---|---|---|
| Frontend → Backend | HTTP + JWT (`src/lib/http.ts`: inyecta el token, refresca solo, redirige a `/login` en 401) | `VITE_API_BASE_URL` en `frontend/.env` |
| Backend → MySQL | JDBC, pool de conexiones de Spring | `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` en `.env` |
| Esquema de la base | **Flyway** aplica las migraciones solo al arrancar el backend | `backend/src/main/resources/db/migration/` (última: V48) |
| Sesión de usuario | JWT de 15 min + refresh token opaco de 7 días con rotación | `JWT_SECRET`, `JWT_ACCESS_TTL_MINUTES`, `JWT_REFRESH_TTL_DAYS` |
| Aislamiento por empresa | Columna `tenant_id` + filtro de Hibernate por request + guardia a nivel repositorio | Automático — no se expone en la UI |
| Archivos adjuntos | Filesystem dentro del contenedor, volumen `adjuntos_data` (nunca en la base) | `ADJUNTOS_DIR=/data/adjuntos` |

### 2.2 Conexiones externas: **ninguna**

Esto es importante y está verificado: **el backend no hace ni una sola llamada HTTP a un servicio externo**. No hay `RestTemplate`, ni `WebClient`, ni `HttpClient`, ni cliente de mail en todo el código.

Concretamente, esto significa que **no** existe:

| Integración que NO hay | Cómo se resuelve hoy |
|---|---|
| ARCA/AFIP (emisión de facturas) | Por diseño: el sistema **registra** comprobantes ya emitidos, no los emite |
| Cotización automática del dólar | El tipo de cambio lo **carga el operador a mano** en cada operación |
| API de bancos | Se importa el **archivo** del resumen (Excel/PDF) que se baja del home banking |
| Envío de emails / notificaciones | Las alertas son solo **in-app** (campanita). La interfaz `AlertChannel` está lista para un canal de email futuro, pero ese canal no existe |
| Almacenamiento en la nube | Los adjuntos van al disco del servidor |

Las únicas "entradas externas" son archivos que sube el usuario:

| Origen | Formato aceptado | Parser |
|---|---|---|
| Banco Galicia (home banking) | Excel **o** PDF | `ParserGalicia` |
| Mercado Pago | Excel | `ParserMercadoPago` |
| Tarjeta VISA Business Galicia | PDF | `ParserTarjeta` |
| Facturas históricas | PDF | `ExtractorFacturaPdf` |

Archivos reales disponibles para probar, ya en el repo: `facturasyresumenes/` → `VENTAS/`, `COMPRAS/`, `RESUMENES BANCARIOS/` (7 meses: Octubre a Abril), y `EECC.pdf`.

### 2.3 Mapa de pantallas

Todas las rutas navegables (útil como checklist de cobertura):

**Maestros:** `/monedas` · `/tipos-cambio` · `/jurisdicciones` · `/clientes` · `/proveedores` · `/proyectos` (+ detalle) · `/comisionistas` · `/categorias` · `/rubros` · `/conceptos` · `/tipos-costo` · `/cuentas-bancarias` · `/tarjetas-credito` (+ detalle)

**Contabilidad:** `/contabilidad` (plan de cuentas) · `/contabilidad/asientos` · `/contabilidad/mayor/:cuentaId`

**Facturación:** `/facturacion/ventas` · `/compras` · `/cobros` · `/pagos` · `/cuentas-por-cobrar` · `/cuentas-por-pagar` · `/mapeo-cuentas` · `/importacion-historica`

**Bancos:** `/bancos/movimientos` · `/bancos/importacion` · `/bancos/conciliacion` · `/bancos/reglas-clasificacion-consumo`

**Reportes:** `/reportes/balance-sumas-y-saldos` · `/reportes/estado-resultados` · `/reportes/mapeo-rubro-linea-er`

**Impuestos:** `/impuestos/iva` · `/impuestos/iibb`

**Presupuesto:** `/presupuesto/vencimientos` · `/pagos` (compromisos) · `/flujo-caja` · `/inversiones` (+ detalle)

**Otros:** `/` (dashboard) · `/pendientes` · `/periodos` · `/seguridad` · `/auditoria`

---

## 3. Plan de pruebas manuales

Ocho sesiones ordenadas por dependencia: cada una asume que las anteriores pasaron. Podés hacerlas en días distintos.

> **Convención:** marcá cada punto como ✅ / ❌ / ⚠️ (funciona pero raro). Para los ❌ y ⚠️, anotá: qué pantalla, qué hiciste, qué esperabas, qué pasó, y el mensaje de error textual si hubo.

### Sesión 1 — Acceso, permisos y navegación (~30 min)

Objetivo: que entre, que los roles funcionen, y que ninguna pantalla explote al abrirla.

1. Login con `admin@montanaritech.com`. Probá también una contraseña incorrecta → tiene que rechazarla.
2. Recorré **las 30+ rutas** de la lista de arriba, una por una. En esta sesión no cargues nada: solo confirmá que cada pantalla abre, muestra datos (o un "sin resultados" prolijo) y no queda en blanco ni tira error.
3. Dejá la sesión abierta 20 minutos sin hacer nada, y después hacé clic en algo. El token de acceso dura 15 minutos: el sistema debería renovarlo solo y seguir andando, **sin** mandarte al login.
4. Creá desde `/seguridad` dos usuarios de prueba: uno **CARGA** y uno **LECTURA**.
5. Entrá con el usuario **LECTURA** → no debería poder crear/editar/borrar nada.
6. Entrá con el usuario **CARGA** → debería poder cargar, pero **no** ver `/periodos` ni `/seguridad` (son solo de admin).

### Sesión 2 — Maestros (~45 min)

Objetivo: el ABM básico y las validaciones.

1. **Clientes**: creá uno nuevo, editalo, desactivalo, reactivalo. Probá guardar sin CUIT y con un CUIT inválido.
2. **Proveedores**: lo mismo.
3. **Proyectos**: creá uno tipo *Argentina* y otro tipo *Exterior* (esta distinción cambia la cascada de presupuesto de la Sesión 7).
4. **Cuentas bancarias**: revisá que estén las reales cargadas con su saldo inicial. Creá una de prueba.
5. **Plan de cuentas** (`/contabilidad`): navegá el árbol. Probá crear una cuenta hija.
6. **Tipos de cambio**: cargá una cotización del día de hoy — te va a servir en la Sesión 4.
7. En cualquiera de estas pantallas, probá **eliminar** algo que ya tenga movimientos asociados → tiene que rechazarlo con un mensaje claro, no romperse.

### Sesión 3 — Facturas (~1 h)

Objetivo: el corazón del sistema — que confirmar genere el asiento correcto.

1. **Factura de venta en pesos**: cargala con 2 líneas (una al 21%, otra al 10,5%). Verificá que los totales (Neto/IVA/Total) den bien.
2. Guardala como borrador, salí de la pantalla, volvé y editala → los datos tienen que estar.
3. **Confirmala**. Anotá el N° de asiento que aparece. Andá a `/contabilidad/asientos`, buscá ese asiento y verificá que:
   - Balancea (Σdebe = Σhaber).
   - Las cuentas usadas tienen sentido (CxC al debe, ventas e IVA débito al haber).
4. Intentá **editar** la factura ya confirmada → tiene que rechazarlo.
5. **Anulala** con un motivo. Verificá que quede como anulada (no que desaparezca) y que el asiento también se haya anulado.
6. **Factura de venta en USD**: cargá una con tipo de cambio, confirmala, y revisá que el asiento esté valuado en pesos al TC que pusiste.
7. **Factura de compra**: igual, más la sección de **percepciones sufridas** (cargá una percepción de IVA y una de IIBB).
8. Probá confirmar una factura **sin líneas** → mensaje: *"La factura no tiene ningún importe a contabilizar"*.
9. Revisá `/facturacion/cuentas-por-cobrar` y `/cuentas-por-pagar`: la factura nueva tiene que aparecer con su saldo pendiente.

### Sesión 4 — Cobros, pagos y diferencia de cambio (~1 h)

Esta es la parte con más lógica fina. Vale la pena ir despacio.

1. **Cobro simple en pesos**, imputado 100% contra una factura de la Sesión 3. Confirmalo. Verificá en CxC que la factura quedó saldada.
2. **Cobro parcial**: imputá solo la mitad de otra factura. En CxC debería quedar con saldo pendiente por la diferencia.
3. **Cobro contra 2 facturas a la vez** (esto es lo que rompía antes del fix B7): confirmá y verificá que el asiento **balancee exacto**, sin diferencias de 1 centavo.
4. **Cobro con anticipo**: cobrá más de lo que imputás. El excedente tiene que quedar como anticipo disponible.
5. Después, usá **"Aplicar anticipo"** contra otra factura del mismo cliente.
6. **Cobro en USD con TC distinto al de la factura**: acá el sistema tiene que generar solo la línea de *diferencia de cambio* (ganada o perdida). Verificá que aparezca en el asiento y que balancee.
7. **Cobro con retenciones sufridas**: cargá una retención de Ganancias. Verificá que "ingresa a fondos" = total − retenciones.
8. **Pagos**: repetí los casos 1, 2 y 6 del lado de proveedores.
9. Probá imputar contra una factura **en otra moneda** → tiene que rechazarlo.
10. Probá imputar **más que el saldo pendiente** de la factura → tiene que rechazarlo.

### Sesión 5 — Bancos: importación, bandeja y conciliación (~1 h)

1. Andá a `/bancos/importacion`. Importá el resumen de Galicia de un mes desde `facturasyresumenes/RESUMENES BANCARIOS/`.
2. En la previsualización, verificá que las filas se lean bien (fecha, descripción, importe). Si alguna fila viene sin fecha, completala ahí.
3. **Importá el mismo archivo dos veces** → la segunda vez todas las filas tienen que marcarse como *"Ya importado anteriormente"*, sin duplicar nada.
4. Importá un resumen de **Mercado Pago** y uno de **tarjeta** (PDF).
5. Probá subir un archivo **equivocado** (ej. el Excel de Galicia eligiendo origen "Mercado Pago") → tiene que dar un error claro, no romperse.
6. En `/bancos/movimientos`, sobre los movimientos pendientes probá las 4 acciones, una en cada movimiento distinto:
   - **Confirmar** (con cuenta sugerida)
   - **Imputar** (eligiendo la cuenta a mano)
   - **Asociar** (a un asiento ya existente y confirmado)
   - **Descartar** (con motivo)
7. Verificá que un movimiento ya resuelto **no se pueda volver a tocar**.
8. Andá a `/bancos/conciliacion`, elegí una cuenta y un rango. Revisá el resumen (saldo banco vs. saldo sistema) y probá **aceptar** una sugerencia de match y **rechazar** otra.

### Sesión 6 — Impuestos y cierre de períodos (~45 min)

1. `/impuestos/iva`: elegí un mes con datos (ej. marzo/2026) y mirá la **previsualización** antes de liquidar.
2. Creá la liquidación. Revisá los componentes de las dos etapas y los tres resultados (a pagar / saldo técnico / libre disponibilidad).
3. Hacé un **ajuste manual** sin motivo → tiene que rechazarlo. Después ponelo con motivo → tiene que aceptarlo.
4. **Confirmá** la liquidación y verificá el asiento generado.
5. **Des-confirmala** con motivo y verificá que el asiento quede anulado.
6. `/impuestos/iibb`: lo mismo, revisando el reparto por jurisdicción, el coeficiente y las deducciones. Prestá atención a las **advertencias** que aparezcan arriba (ventas sin jurisdicción, coeficientes que no suman 1, etc.).
7. `/periodos`: generá los períodos, **cerrá un mes** con motivo.
8. Intentá cargar una factura con fecha de ese mes cerrado:
   - Con el usuario **CARGA** → tiene que bloquearlo.
   - Con el **admin** → tiene que ofrecerte el "Confirmar igual" con motivo.
9. **Reabrí** el período.

### Sesión 7 — Reportes, presupuestos y exportaciones (~45 min)

1. `/reportes/balance-sumas-y-saldos`: **tiene que decir que balancea, con diferencia $0,00**. Este es el chequeo más importante de todos.
2. Probá el **drill-down**: clic en una cuenta → te lleva al Mayor de esa cuenta.
3. `/reportes/estado-resultados`: revisá las 4 vistas y el comparativo. Fijate si hay conceptos "sin mapear" (es un gap conocido del seed, no un bug).
4. `/presupuesto/flujo-caja`: revisá real vs. proyectado.
5. En un proyecto tipo **Argentina**, cargá el presupuesto (líneas de costo + margen) y revisá la cascada de cálculo.
6. En uno tipo **Exterior**, lo mismo — la cascada tiene que ser distinta (COMEX).
7. Pestaña **Rentabilidad** de un proyecto con facturas reales: revisá ingresos, egresos, margen real.
8. **Exportá a Excel y a PDF** desde al menos 4 pantallas distintas. Abrí los archivos y verificá que: tengan el encabezado de la empresa, respeten el filtro que tenías en pantalla, y que las tildes/ñ se vean bien.
9. `/presupuesto/vencimientos`: probá **"Generar ahora"**, y sobre un vencimiento probá marcar pagado / reprogramar / cancelar.
10. Corré **"Generar ahora"** dos veces seguidas → la segunda no debería duplicar nada.

### Sesión 8 — Transversales (~30 min)

1. **Lupita** (Ctrl+K): buscá un cliente por nombre, una factura por número, algo por CUIT, y un importe. Verificá que al hacer clic en un resultado te lleve a donde corresponde.
2. **Alertas** (campanita): revisá qué alertas hay activas y marcá una como leída.
3. `/pendientes`: creá un pendiente, cambiale el estado, filtralo.
4. `/auditoria`: verificá que **todo lo que hiciste en las sesiones anteriores aparezca registrado** (usuario, fecha, acción, datos anteriores/nuevos).
5. **Chequeo final de integridad**: volvé al Balance de sumas y saldos. Tiene que seguir balanceando. Si cambió el total respecto de los $305.621.023,15 iniciales, es esperable (cargaste cosas nuevas), pero **la diferencia tiene que seguir siendo $0,00**.
6. Probá la app en una ventana angosta / celular, para ver cómo se comporta el diseño.

---

## 4. Pendientes

### 4.1 Pendiente de **conectar** (no está roto, nunca se conectó)

| Qué | Estado | Cuándo hace falta |
|---|---|---|
| **Destino del backup fuera del servidor** | `ops/backup/offsite_hook.sh` es un stub: hoy solo loguea "sin destino configurado", o copia a una carpeta local si definís `SECONDARY_BACKUP_DIR` | **Antes de producción.** Sin esto, si se incendia el VPS no hay backup recuperable |
| **Canal de email para alertas** | Solo existe el canal in-app (campanita). La interfaz `AlertChannel` está lista para agregar email sin tocar el motor | Cuando el equipo quiera avisos por mail |
| **Cotización automática del dólar** | El TC lo carga el operador a mano en cada operación | Opcional — es una decisión de producto, no una deuda técnica |
| **Logo de la empresa** | Hay un slot configurable (`Tenant.logoClasspath`) que hoy está vacío | Cuando quieran que salga en los PDF exportados |

### 4.2 Pendiente de **desarrollar / revisar** (deuda conocida, documentada)

| # | Qué | Impacto |
|---|---|---|
| A2 | Posible imprecisión del tipo de cambio efectivo en imputaciones grandes | No se pudo reproducir; quedó abierto |
| A5 | Datos personales reales en fixtures de test | Higiene, no afecta el funcionamiento |
| — | 4 índices FULLTEXT creados pero **sin usar** (Lupita busca con LIKE) | Peso muerto en escritura, cero impacto funcional |
| — | Override de período cerrado **no expuesto** en 4 pantallas (movimientos bancarios, pago de tarjeta, liquidación IVA/IIBB) | Ni el admin puede forzar ahí: quedan bloqueadas sin excepción en período cerrado |
| — | Ruta `/ejemplo-formulario` | Sobrante del scaffolding inicial (F1.4) — conviene borrarla antes de producción |
| — | Rubros sin mapear en el Estado de Resultados | Gap del seed inicial; el usuario decidió no tocarlo |
| — | **Capturas de pantalla del manual de usuario** | Los 11 capítulos tienen placeholders `[CAPTURA]`. Buen momento para sacarlas: mientras hacés estas pruebas |

### 4.3 Credenciales y secretos

| Qué | Estado hoy | Acción |
|---|---|---|
| Admin `changeme123` | Sembrado por Flyway, público en el repo | **Rotar** apenas se despliegue (y ojalá también en local si vas a cargar datos sensibles) |
| `JWT_SECRET` | El de tu `.env` local | Generar **uno nuevo y distinto** para producción |
| Contraseñas de MySQL | Las del `.env` local | Generar nuevas para producción |
| `.env` | Correctamente ignorado por git (verificado) | No commitearlo nunca |
| Credenciales bancarias en el Excel original | Estaban expuestas en el archivo fuente que trajo el cliente (hallazgo de F10.1) | Conviene avisarle al equipo que las rote del lado del banco |

### 4.4 Lo que corresponde al montar el VPS

Todo esto ya está **escrito y probado**, pero requiere el servidor real. El procedimiento completo está en [RUNBOOK.md](RUNBOOK.md).

1. **Contratar el VPS** (Linux + Docker + Docker Compose). Todavía no hay ninguno.
2. **Conseguir el certificado TLS** — la decisión fue certificado provisto manualmente, sin Let's Encrypt. Hay que colocarlo como `./certs/fullchain.pem` y `./certs/privkey.pem` (esos dos nombres exactos).
3. **Crear el `.env` de producción** con secretos nuevos (no reusar los de dev).
4. **Desplegar**: `docker compose -f docker-compose.prod.yml up -d --build`.
5. **Correr el smoke test**: `ops/smoke_test.sh`.
6. **Rotar el admin sembrado** inmediatamente después del primer login.
7. **Activar la CSP**: el header hoy está en modo `Report-Only`. Navegá la app, confirmá en la consola del navegador que no se bloquea nada, y recién ahí pasalo a modo enforcing.
8. **Programar el backup** en el cron del host (`0 3 * * * ops/backup/backup.sh`) — incluye verificación automática de restore cada noche.
9. **Conectar el destino de backup externo** (punto 4.1).
10. **Poner un recordatorio de calendario** para la fecha de vencimiento del certificado: no hay renovación automática.
11. **Ajustar los límites de recursos** del `docker-compose.prod.yml` al tamaño real del VPS contratado (hoy son valores genéricos de arranque).
12. **Abrir solo los puertos 80 y 443** en el firewall. Los de MySQL (3306) y backend (8080) **no** deben abrirse — el compose de producción ya no los publica.

---

## Cómo reportar lo que encuentres

Para cada problema, con esto alcanza para poder arreglarlo sin ida y vuelta:

```
Pantalla:    /facturacion/cobros
Qué hice:    Cargué un cobro en USD imputado a 2 facturas y confirmé
Esperaba:    Que genere el asiento
Pasó:        Error rojo "La suma de las imputaciones no puede superar el total cobrado"
Datos:       Total 1.500 USD, TC 1.240, imputé 800 + 700
```

Si algo rompe el balance contable, avisá **antes** de seguir cargando: conviene frenar y restaurar el backup en vez de arrastrar el error.
