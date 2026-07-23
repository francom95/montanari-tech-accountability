# Montanari Tech — Sistema de Gestión Contable
Plan de trabajo: carpeta ./plan (leer 00_README.md primero).
Contexto y stack: definidos en cada MD del plan y en ./plan/00_plantillas.md.
Reglas innegociables: sección "Reglas de negocio" de cada paso.
Ejecutar SIEMPRE un paso por vez, con el modelo indicado en su encabezado.
Salidas de diseño se guardan en ./outputs con el ID del paso.

## Grafo de conocimiento (graphify)
Mantener `./graphify-out/` siempre al día durante la sesión, de forma incremental.
- **Después de CADA acción que modifique archivos** (crear/editar/borrar código, migraciones, docs del plan, outputs), actualizar el grafo con `/graphify . --update` (incremental: solo re-extrae lo cambiado; el AST de código es gratis y determinista). No esperar al commit.
- **Además, antes de commitear y antes de pushear**, correr `/graphify . --update` para que el grafo refleje exactamente lo que se versiona.
- Usar SIEMPRE `--update`, nunca el pipeline completo, salvo que el grafo no exista o esté corrupto.
- Agrupar varias ediciones de un mismo cambio atómico en una sola corrida de `--update` (no una por línea).
- Nota: esta regla la ejecuta el asistente dentro de la sesión; ediciones hechas fuera de Claude Code no disparan la actualización.