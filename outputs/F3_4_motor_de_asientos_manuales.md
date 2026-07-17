# F3.4 — Motor de asientos manuales

**Paso:** 19 de 55 · **Fase:** F3 — Núcleo contable · **Modelo:** Sonnet 5 · **Depende de:** F3.2
**Checkpoint humano:** No.

## Alcance de este paso (y qué queda para después)

F3.1 (§3-§4) describe todo el motor contable de una sola vez, pero el plan reparte su implementación en varios pasos. F3.4 cubre **solo la carga manual**:

- Crear/editar/eliminar un asiento en **borrador** (sin ninguna validación contable — F3.1 §3.5).
- **Confirmar**: numeración interna, balanceo obligatorio, consistencia multimoneda, cuentas imputables/activas.
- Grilla de líneas con dimensiones analíticas opcionales (proyecto, etapa, cliente, proveedor, destino de fondos).

**Explícitamente fuera de alcance de F3.4** (pasos futuros ya nombrados en el plan):
- Búsqueda avanzada, **duplicación**, **edición de confirmados** y **anulación** → **F3.5**.
- Mayores, saldos y la advertencia de "saldo contrario al esperado" (CP-20) → **F3.6**.
- Generadores automáticos (factura/cobro/pago, diferencia de cambio real) → **F4.x**.
- Guarda de período cerrado → **F9.3** (la entidad `Periodo` todavía no existe; el ítem 5 del checklist de confirmación de F3.1 §3.4 no aplica todavía porque no hay período que pueda estar cerrado).

## Qué se hizo

**Backend** (paquete nuevo `contabilidad.asiento`):
- `V18__contabilidad_asiento.sql`: tablas `secuencia` (con seed de la fila `ASIENTO`), `asiento`, `asiento_linea`.
- `Asiento` / `AsientoLinea` (JPA) + `OrigenAsiento` (enum cerrado transcripto de F3.1 §4.4; F3.4 solo produce `MANUAL`).
- `AsientoService`: `crearBorrador`, `editarBorrador`, `eliminarBorrador`, `confirmar` — checklist de confirmación completo (balanceo, XOR debe/haber, cuenta imputable/activa, consistencia ARS, TC automático).
- `NumeradorAsientoPersistente`: reemplaza el placeholder `NumeradorAsientoEnMemoria` del molde F1.8 — numeración por `secuencia` con lock pesimista (`SELECT ... FOR UPDATE`).
- `CuentaContableService.tieneMovimientos`: se completó el hook que F3.1 había dejado pendiente — ahora consulta `asiento_linea` de verdad.

**Frontend:** `types/asiento.ts`, `hooks/use-asiento.ts`, `pages/asientos-page.tsx` (listado + formulario con grilla `useFieldArray`, un `<select>` por cuenta imputable+activa a modo de autocompletar, totales debe/haber en vivo con la diferencia resaltada). Ruta `/contabilidad/asientos` + ítem de navegación.

## Casos de prueba de F3.1 §10 cubiertos (los que aplican a carga manual)

| CP | Cubierto en | Nota |
|---|---|---|
| CP-01 (balanceado ARS) | `AsientoServiceTest` | numeración + TC normalizado a 1 |
| CP-02 (desbalanceado → borrador OK, confirmar falla) | `AsientoServiceTest` | `ASIENTO_NO_BALANCEA` |
| CP-03 a/b/c (madre, XOR, incompleto) | `AsientoServiceTest` | `CUENTA_NO_IMPUTABLE`/`LINEA_DEBE_XOR_HABER`/`ASIENTO_INCOMPLETO`/`ASIENTO_SIN_LINEAS` |
| CP-04 (numeración sin huecos) | `NumeradorAsientoPersistenteTest` | la garantía vive en la secuencia, no en el service |
| CP-05 (fecha intermedia) | `AsientoServiceTest` | no hay ninguna comparación cronológica en el código |
| CP-19 (TC faltante / cargado) | `AsientoServiceTest` | + 2 casos extra: `IMPORTE_ORIGINAL_REQUERIDO`, `MONTO_ARS_INCONSISTENTE` |

CP-06 a CP-18 y CP-20 pertenecen a F3.5/F3.6/F4.x (ver tabla de alcance) y no se testean acá.

## Decisiones tomadas (sin checkpoint, documentadas para trazabilidad)

1. **TC automático simplificado.** F3.1 habla de un "parámetro de sistema `FUENTE_TC_DEFAULT`" que no existe todavía. Cuando una línea en moneda extranjera no trae TC manual, se busca la primera `TipoCambio` activa para (moneda, fecha) sin filtrar por criterio, y se usa su `valorVenta`. Si no hay ninguna, `TC_FALTANTE`.
2. **"Destino de fondos" = `CuentaBancaria`.** F3.1 lo llama `cuenta_financiera_id` de forma genérica; se resolvió como FK directa a `cuenta_bancaria` (cubre banco/caja/Mercado Pago vía su enum `TipoCuenta`), no como referencia polimórfica.
3. **Sin CHECK de fila para "debe XOR haber".** F3.1 lo pide como CHECK de base de datos, pero eso bloquearía guardar un borrador con una línea todavía en blanco (ambos en cero), lo cual el propio F3.1 §3.5 permite explícitamente. Se validó en el service al confirmar; la BD solo garantiza no-negatividad.
4. **6ta categoría / origen `OrigenAsiento` predeclarado completo** (`MANUAL`, `AJUSTE`, `APERTURA`, `FACTURA_VENTA`, etc.) aunque F3.4 solo produce `MANUAL` — mismo criterio que `EstadoDocumento`/`AccionAuditoria` del molde F1.8: es un dominio ya cerrado por el diseño de F3.1, y al ser columna `VARCHAR`, agregar valores más adelante no pide migración.

## Verificación realizada

- **Backend compila** bajo JDK 21 (`test-compile` y `test` completos, exit 0).
- **156 tests corridos, 155 en verde.** El único que falla (`ContableApplicationTests`) es el conocido problema de Testcontainers↔Docker Desktop en esta máquina (documentado en memoria), no relacionado con este cambio.
- **Migración V18 aplicada contra MySQL 8 real** (cadena completa V1→V18): tablas, FKs y CHECK constraints correctos. Smoke test manual con las cuentas del seed de F3.3 (`1.1.2001` Banco Galicia CC / `4.1.2001` Ingresos por ventas): asiento balanceado insertado, confirmado con `numero` asignado **fuera de orden de creación** (Borrador B creado después de A pero confirmado con número 1), verificando CP-04. El CHECK de no-negatividad rechaza un `debe` negativo.
- **Frontend compila** (`tsc -b`, exit 0).
