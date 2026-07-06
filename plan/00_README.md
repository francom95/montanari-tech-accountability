# 00 — README: cómo ejecutar este plan

**Proyecto:** Sistema de Gestión Contable Montanari Tech.

## Cómo usar estos archivos

1. Ejecutá los pasos **en orden numérico** (respetan las dependencias).
2. Abrí una sesión con el **modelo indicado en el encabezado** de cada archivo.
3. Pegá/adjuntá el archivo del paso + las **entradas** que lista (salidas de pasos previos, archivos del equipo).
4. Si el paso usa plantillas (PL-x), adjuntá también `00_plantillas.md` y el código molde de F1.8.
5. Si el paso tiene **checkpoint humano**, no avances al siguiente sin esa validación.
6. Guardá la salida de cada paso: es entrada de los siguientes.

## Asignación de modelos (actualizada)

| Modelo | Pasos | Uso |
|---|---|---|
| 🟣 Claude Fable 5 | 3 | F1.1 arquitectura, F3.1 motor contable, F11.1 revisión final — los tres pasos de mayor apalancamiento |
| 🔴 Claude Opus 4.8 | 3 | F4.1 reglas de asientos, F6.1 IVA, F6.2 IIBB — lógica contable/fiscal sensible |
| 🟠 Claude Sonnet 5 | 37 | Implementación estándar |
| 🟢 Claude Haiku 4.5 | 12 | Volumen sobre plantilla |

**Cambio respecto del plan anterior:** F1.1, F3.1 y F11.1 suben de Opus 4.8 a **Fable 5**. Motivo: se ejecutan una sola vez, definen (o auditan) todo lo demás, y un error ahí es el más caro del proyecto. El resto de Opus queda igual.

## Insumos del equipo que bloquean pasos (conseguir cuanto antes)

- Hoja de fórmulas de presupuesto del Excel → bloquea **F2.6**.
- Resúmenes reales de Galicia, Mercado Pago y tarjeta → bloquean **F5.2**.
- Excel completo actual → bloquea **F10.1** (y F3.3 necesita la hoja del plan de cuentas).
- Una liquidación real de IVA y de IIBB del contador → recomendadas para **F6.1/F6.2**.
- Logo → no bloquea nada; se integra en parámetros del sistema (F7.1 deja el slot).

## Índice de pasos

| # | ID | Paso | Modelo | Checkpoint |
|---|---|---|---|---|
| 01 | F1.1 | Arquitectura global y modelo de datos contable | Fable 5 | ✅ |
| 02 | F1.2 | Decisiones de tooling (repo, build, UI) | Sonnet 5 | ✅ |
| 03 | F1.3 | Scaffolding backend + Docker Compose | Sonnet 5 | — |
| 04 | F1.4 | Scaffolding frontend | Sonnet 5 | — |
| 05 | F1.5 | Autenticación, roles y autorización | Sonnet 5 | — |
| 06 | F1.6 | Infraestructura de auditoría | Sonnet 5 | — |
| 07 | F1.7 | CI básico | Haiku 4.5 | — |
| 08 | F1.8 | Definición formal de las 5 plantillas (código molde) | Sonnet 5 | ✅ |
| 09 | F2.1 | CRUDs simples en lote (batch 1 de maestros) | Haiku 4.5 | — |
| 10 | F2.2 | CRUD Clientes | Haiku 4.5 | — |
| 11 | F2.3 | CRUD Proveedores | Haiku 4.5 | — |
| 12 | F2.4 | Cuentas bancarias, cuentas de dinero y tarjetas (maestros con saldo inicial) | Sonnet 5 | — |
| 13 | F2.5 | Proyectos y etapas | Sonnet 5 | — |
| 14 | F2.6 | Presupuesto estimado por proyecto | Sonnet 5 | ✅ |
| 15 | F2.7 | Comisionistas | Sonnet 5 | — |
| 16 | F3.1 | Diseño del motor contable | Fable 5 | ✅ |
| 17 | F3.2 | Plan de cuentas (implementación) | Sonnet 5 | — |
| 18 | F3.3 | Seed del plan de cuentas inicial | Haiku 4.5 | ✅ |
| 19 | F3.4 | Motor de asientos manuales | Sonnet 5 | — |
| 20 | F3.5 | Búsqueda, duplicación, edición y anulación de asientos | Sonnet 5 | — |
| 21 | F3.6 | Mayores contables | Sonnet 5 | — |
| 22 | F4.1 | Reglas de asientos automáticos e imputación de cobros/pagos | Opus 4.8 | ✅ |
| 23 | F4.2 | Facturas de venta | Sonnet 5 | — |
| 24 | F4.3 | Facturas de compra | Sonnet 5 | — |
| 25 | F4.4 | Cobros y pagos | Sonnet 5 | ✅ |
| 26 | F4.5 | Vistas de cuentas por cobrar y por pagar | Haiku 4.5 | — |
| 27 | F4.6 | Importación de facturación histórica | Sonnet 5 | — |
| 28 | F5.1 | Movimientos bancarios y bandeja 'pendiente de revisar' | Sonnet 5 | — |
| 29 | F5.2 | Parsers de resúmenes (Galicia, Mercado Pago, tarjetas) | Sonnet 5 | ✅ |
| 30 | F5.3 | Conciliación bancaria | Sonnet 5 | — |
| 31 | F5.4 | Tarjetas de crédito (operatoria completa) | Sonnet 5 | — |
| 32 | F6.1 | Lógica de IVA (liquidación mensual) | Opus 4.8 | ✅ |
| 33 | F6.2 | Lógica de IIBB (liquidación multi-jurisdicción) | Opus 4.8 | ✅ |
| 34 | F6.3 | Asociación de impuestos a proyectos | Sonnet 5 | — |
| 35 | F7.1 | Infraestructura de exportación consolidada | Sonnet 5 | — |
| 36 | F7.2 | Balance de sumas y saldos | Sonnet 5 | — |
| 37 | F7.3 | Estado de resultados | Sonnet 5 | — |
| 38 | F7.4 | Reporte detallado por proyecto (rentabilidad) | Sonnet 5 | ✅ |
| 39 | F7.5 | Dashboard | Sonnet 5 | — |
| 40 | F7.6 | Exportaciones restantes en lote | Haiku 4.5 | — |
| 41 | F8.1 | Calendario de vencimientos | Sonnet 5 | — |
| 42 | F8.2 | Presupuesto de pagos | Haiku 4.5 | — |
| 43 | F8.3 | Flujo de caja real y proyectado | Sonnet 5 | — |
| 44 | F8.4 | Inversiones (Fondos Fima y similares) | Haiku 4.5 | — |
| 45 | F8.5 | Pendientes administrativos | Haiku 4.5 | — |
| 46 | F9.1 | Motor de alertas | Sonnet 5 | — |
| 47 | F9.2 | Búsqueda global 'Lupita' | Sonnet 5 | — |
| 48 | F9.3 | Períodos contables y cierre | Sonnet 5 | — |
| 49 | F10.1 | Mapeo Excel → sistema | Sonnet 5 | ✅ |
| 50 | F10.2 | Scripts de importación por hoja | Haiku 4.5 | — |
| 51 | F10.3 | Saldos iniciales y arranque (asiento de apertura) | Sonnet 5 | ✅ |
| 52 | F11.1 | Revisión final de seguridad e integridad contable | Fable 5 | ✅ |
| 53 | F11.2 | Fixes + performance | Sonnet 5 | — |
| 54 | F11.3 | Despliegue productivo | Sonnet 5 | — |
| 55 | F11.4 | Documentación de usuario | Haiku 4.5 | — |
