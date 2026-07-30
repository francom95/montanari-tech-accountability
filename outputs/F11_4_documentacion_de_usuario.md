# F11.4 — Documentación de usuario

Modelo asignado: Haiku 4.5. Usado: Sonnet 5 (discrepancia consultada explícitamente; el usuario eligió seguir con Sonnet 5, mismo patrón que F4.5 y F10.2).

> Nota sobre el "molde de referencia (F1.8)" que menciona el encabezado del paso: F1.8 define el molde de **código** (plantillas PL-1..PL-5) para entidades CRUD — no aplica literalmente a este paso, que es 100% documentación de usuario. Se interpretó la instrucción "no tomes decisiones de diseño" como: no inventar módulos, pantallas ni mensajes que no existan en el sistema real — cada dato del manual (nombres de campos, botones, mensajes de error) se extrajo del código real (frontend + backend), no de memoria ni de suposición.

## Qué se construyó

`docs/manual-usuario/` — 13 archivos Markdown (uno por módulo + índice + preguntas frecuentes) y un PDF compilado:

1. [00_indice.md](../docs/manual-usuario/00_indice.md) — portada e ideas generales (borrador/confirmado/anulado, bandeja de importación, cierre de períodos, multimoneda).
2. [01_facturas_venta_compra.md](../docs/manual-usuario/01_facturas_venta_compra.md)
3. [02_cobros_y_pagos.md](../docs/manual-usuario/02_cobros_y_pagos.md)
4. [03_resumenes_bancarios_y_bandeja.md](../docs/manual-usuario/03_resumenes_bancarios_y_bandeja.md)
5. [04_conciliacion_bancaria.md](../docs/manual-usuario/04_conciliacion_bancaria.md)
6. [05_liquidacion_iva_e_iibb.md](../docs/manual-usuario/05_liquidacion_iva_e_iibb.md)
7. [06_cierre_de_periodos.md](../docs/manual-usuario/06_cierre_de_periodos.md)
8. [07_vencimientos_y_alertas.md](../docs/manual-usuario/07_vencimientos_y_alertas.md)
9. [08_presupuestos_y_rentabilidad.md](../docs/manual-usuario/08_presupuestos_y_rentabilidad.md)
10. [09_busqueda_lupita.md](../docs/manual-usuario/09_busqueda_lupita.md)
11. [10_pendientes_administrativos.md](../docs/manual-usuario/10_pendientes_administrativos.md)
12. [11_exportaciones.md](../docs/manual-usuario/11_exportaciones.md)
13. [12_preguntas_frecuentes.md](../docs/manual-usuario/12_preguntas_frecuentes.md) — los 10 errores de carga más probables, con "cuándo pasa" y "cómo corregirlo".

**PDF compilado**: `docs/manual-usuario/Manual_de_Usuario_Montanari_Tech.pdf` (27 páginas, con portada, índice con números de página reales y tildes/ñ verificadas carácter por carácter).

## Método de trabajo

Antes de escribir una sola línea del manual, se corrieron **4 sub-agentes de investigación en paralelo** sobre el código real (frontend `pages/`, DTOs con Bean Validation, y las clases de servicio del backend), cubriendo los 11 módulos pedidos. Cada agente devolvió, verbatim: nombres exactos de campos y botones en español, columnas de tablas, estados de cada entidad (enums reales), y los mensajes de error/advertencia tal cual los emite el backend (`NegocioException`, `RecursoNoEncontradoException`, `PeriodoCerradoException`, etc.). El manual se escribió a partir de esos hechos, no de una idea general de "cómo debería funcionar" — esto es lo que permite que las 10 preguntas frecuentes citen mensajes reales del sistema en vez de errores inventados.

## Por qué el PDF se armó con reportlab y no con un conversor directo

No hay `pandoc` instalado en el entorno. Se generó el PDF con un script Python (`reportlab`, biblioteca ya usada en el proyecto para las exportaciones PL-3 del propio sistema) que parsea los 13 Markdown y arma: portada, tabla de contenidos con `TableOfContents` de reportlab (paginado real vía `doc.multiBuild`), títulos con marcadores de página (bookmarks), listas, tablas simples y los placeholders de captura (`[CAPTURA] ...`) en itálica. Se verificó el resultado con `pdftotext -enc UTF-8` — texto completo, 27 páginas, tildes/ñ/símbolos correctos, TOC con números de página reales.

## Alcance y limitaciones

- **No hay capturas de pantalla reales** — cada lugar donde correspondería una queda marcado con un placeholder itálico (`[CAPTURA] (Captura: ...)`) describiendo qué pantalla debería mostrarse ahí. Insertar las imágenes reales queda pendiente para cuando el sistema esté desplegado y estable (ver [F11.3](F11_3_despliegue_productivo.md)) — recomendado hacerlo con el sistema ya en producción, para que las capturas reflejen la versión final.
- El manual documenta el sistema **tal como está hoy** (post-F11.3) — no incluye funcionalidad planeada a futuro.
- Las 10 preguntas frecuentes son una selección curada de los mensajes más probables de encontrar en el uso diario (no un listado exhaustivo de cada `NegocioException` del sistema); cada capítulo tiene además su propia sección corta de "Errores más comunes" específica de ese módulo, para los casos menos frecuentes.

## Verificación

- Los 13 archivos Markdown compilan a un único PDF sin errores.
- Verificación de contenido completo con `pdftotext` en 3 puntos del documento (portada/índice, capítulo intermedio con tabla, últimas páginas con la sección de preguntas frecuentes) — texto correcto y completo en los tres.
- No aplica suite de tests (paso de documentación pura, sin cambios de código).

Con esto se completa el plan de 55 pasos (`plan/00_README.md`).
