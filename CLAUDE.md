# Montanari Tech — Sistema de Gestión Contable
Plan de trabajo: carpeta ./plan (leer 00_README.md primero).
Contexto y stack: definidos en cada MD del plan y en ./plan/00_plantillas.md.
Reglas innegociables: sección "Reglas de negocio" de cada paso.
Ejecutar SIEMPRE un paso por vez, con el modelo indicado en su encabezado.
Salidas de diseño se guardan en ./outputs con el ID del paso.

## Grafo de conocimiento (graphify)
Mantener `./graphify-out/` al día, de forma incremental.
- **Antes de cada commit** (y antes de cada push), correr `/graphify . --update` para que el grafo refleje exactamente lo que se versiona. No hace falta correrlo después de cada edición individual — alcanza con una corrida por commit.
- Usar SIEMPRE `--update`, nunca el pipeline completo, salvo que el grafo no exista o esté corrupto.
- Nota: esta regla la ejecuta el asistente dentro de la sesión; ediciones hechas fuera de Claude Code no disparan la actualización.