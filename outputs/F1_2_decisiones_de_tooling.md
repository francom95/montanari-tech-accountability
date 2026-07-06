# [F1.2] Decisiones de tooling (repo, build, UI)

> **Estado:** pendiente de checkpoint humano (confirmación rápida del equipo).
> **Modelo ejecutor:** Claude Sonnet 5.
> **Entrada usada:** `outputs/F1_1_arquitectura_global_y_modelo_de_datos.md`.
> **Alcance:** 3 ADRs. No hay código en este paso; estas decisiones se materializan recién en F1.3 (backend) y F1.4 (frontend).

---

## ADR-21 — Monorepo con `/backend` y `/frontend`

**Contexto**
El proyecto tiene un solo equipo, un solo deployable por lado (backend Spring Boot, frontend React) y un contrato de API (OpenAPI) que debe mantenerse sincronizado entre ambos. Además, el proyecto entero se ejecuta como una secuencia de 55 pasos delegados a distintos modelos (Fable/Opus/Sonnet/Haiku) en sesiones independientes, cada una con contexto limitado.

**Opciones consideradas**
- **Monorepo** (`/backend`, `/frontend`, `/docker`, `/plan`, `/inputs`, `/outputs` en la raíz) — sugerencia original del equipo.
- **Multi-repo** (`montanari-backend`, `montanari-frontend` separados).

**Decisión: Monorepo.**

1. El contrato OpenAPI vive en el backend y se consume para generar tipos TypeScript del frontend; un monorepo permite que ese ciclo (cambiar endpoint → regenerar tipos → ajustar frontend) sea un solo commit atómico, no una coordinación de dos PRs en dos repos.
2. Cada paso del plan lo ejecuta un modelo distinto en una sesión sin memoria de las anteriores: un único repo raíz con `/plan`, `/inputs`, `/outputs`, `/backend`, `/frontend` le da a cualquier sesión nueva el contexto completo con un solo `checkout`, sin tener que clonar y correlacionar dos repos.
3. Docker Compose orquesta ambos servicios desde una raíz común (`docker-compose.yml` en la raíz referenciando `./backend` y `./frontend` como contexto de build) sin submódulos ni repos externos.
4. Un solo equipo y un solo entorno de despliegue no necesitan versionado independiente por servicio; el monorepo evita drift de versión entre backend y frontend.
5. CI único (F1.7) puede correr backend y frontend en el mismo pipeline con paths-filter, sin duplicar configuración de secrets/runners en dos repos.

**Consecuencias**
- Estructura de carpetas fijada para todo el proyecto (ver §4).
- El `.gitignore` raíz debe cubrir artefactos de ambos stacks (`target/`, `node_modules/`, `dist/`, `.env`).
- Si en el futuro el sistema evoluciona a SaaS multi-cliente con equipos separados de back/front, dividir el monorepo es una operación mecánica (git subtree/filter-repo); no es una decisión irreversible.

---

## ADR-22 — Build tool backend: Maven

**Contexto**
Backend Java 21 + Spring Boot 3.x con Flyway, MapStruct, Lombok y springdoc-openapi ya fijados (F1.1). El proyecto se construye incrementalmente en ~40 pasos de backend ejecutados por modelos distintos (mayoría Sonnet/Haiku), cada uno editando `pom.xml`/`build.gradle` para agregar dependencias o módulos de test.

**Opciones consideradas**
- **Maven** (`pom.xml`, XML declarativo).
- **Gradle** (Groovy o Kotlin DSL, más flexible/rápido para build grandes).

**Decisión: Maven.**

1. Lombok + MapStruct requieren un orden preciso de annotation processors en el compilador; en Maven esto es un fragmento de `maven-compiler-plugin` estándar, estable y ampliamente documentado. En Gradle existen variantes (`annotationProcessor` vs `kapt` vs plugin de terceros) más propensas a inconsistencia cuando 40 pasos, ejecutados por modelos distintos sin memoria compartida, tocan el build file.
2. Agregar una dependencia en Maven es una entrada `<dependency>` con coordenadas Maven Central — bajo riesgo de que un modelo rompa el build al insertarla. Gradle DSL (aunque más expresivo) admite más formas de escribir lo mismo, lo que reduce la previsibilidad entre sesiones.
3. Testcontainers, Flyway y springdoc-openapi tienen ejemplos de referencia mayormente en Maven en la documentación oficial de Spring Boot, lo que reduce alucinación de configuración en modelos más económicos (Haiku).
4. Rendimiento de build no es un factor crítico: proyecto de tamaño moderado (monolito modular, no un mono-repo de decenas de módulos), sin necesidad del build cache incremental agresivo de Gradle.
5. Diferencia de velocidad de build entre Maven y Gradle es irrelevante frente a la ganancia en previsibilidad para ejecución multi-modelo.

**Consecuencias**
- `/backend/pom.xml` único (sin módulos Maven separados; el monolito modular vive en paquetes, no en módulos de build — consistente con ADR-01 de F1.1).
- Wrapper `mvnw`/`mvnw.cmd` committeado para reproducibilidad sin depender de versión de Maven local.
- Cada paso que agregue una dependencia nueva debe declarar versión gestionada por el BOM de Spring Boot (`spring-boot-starter-parent`) cuando exista, para evitar conflictos de versión entre pasos.

---

## ADR-23 — Biblioteca de UI frontend: shadcn/ui + Tailwind CSS

**Contexto**
Frontend React 18 + TypeScript + Vite, con **TanStack Query, React Router, React Hook Form + Zod y TanStack Table ya fijados** por el stack (F1.1, sección "Stack fijado"). El sistema tiene: (a) tablas contables densas (mayores, balance de sumas y saldos, cuentas por cobrar/pagar) con muchas columnas numéricas, filtros y exportación; (b) formularios largos (facturas, asientos multilínea, presupuesto de proyecto); (c) ~12 pasos de CRUDs de volumen delegados a Haiku 4.5 sobre la plantilla PL-2, que deben replicar el mismo patrón entidad por entidad sin reinterpretar una librería nueva cada vez.

**Opciones consideradas**
- **shadcn/ui + Tailwind CSS** — componentes no empaquetados (se copian como código fuente al repo), construidos sobre Radix UI (primitivas accesibles sin estilo) + Tailwind para el styling.
- **MUI (Material UI)** — librería de componentes completa instalada como dependencia, con sistema de theming propio y `DataGrid` incluido (la versión avanzada de filtrado/agrupamiento de `DataGrid` es de pago — MUI X Pro).

**Decisión: shadcn/ui + Tailwind CSS.**

1. **Encaje con TanStack Table (ya fijado):** TanStack Table es headless — no trae UI propia, solo lógica de tabla. shadcn/ui expone primitivas de tabla (`<Table>`, `<TableHeader>`, `<TableRow>`...) que son wrappers finos sobre `<table>` nativo, diseñados exactamente para pintar el resultado de TanStack Table. MUI trae su propio `DataGrid` con gestión de estado propia; usarlo junto con TanStack Table implica no usar `DataGrid` y en cambio maquetar `<Table>` de MUI a mano, perdiendo el beneficio "todo incluido" que sería el argumento a favor de MUI.
2. **Encaje con React Hook Form + Zod (ya fijado):** shadcn/ui provee un componente `Form` que es literalmente un wrapper de RHF + `zodResolver`, con subcomponentes (`FormField`, `FormItem`, `FormMessage`) pensados para ese combo exacto. Con MUI, cada campo requiere envolver manualmente con `Controller` de RHF — más boilerplate repetido en cada uno de los formularios largos del sistema (facturas, asientos, presupuestos).
3. **Volumen con Haiku 4.5:** shadcn/ui no es una dependencia de node_modules sino código fuente vendorizado dentro del repo (`/frontend/src/components/ui`). Una vez que F1.4 scaffoldea las primitivas base (Button, Input, Select, Dialog, Table, Form, Badge, Toast), cada paso de Haiku que arma un CRUD nuevo (PL-2) **lee componentes ya existentes en el propio repo** en vez de razonar sobre la superficie de API de una librería externa — reduce alucinación de props/imports en un modelo económico ejecutando en lote.
4. **Densidad y control visual:** Tailwind da control fino de densidad (padding, tamaño de fuente, alto de fila) necesario para tablas contables con muchas columnas; MUI empuja hacia su sistema de spacing/theme propio, que cuesta más "apretar" para pantallas densas sin pelear contra el theme.
5. **Costo/licencia:** las funciones avanzadas de grilla (agrupamiento, pivot, export nativo) que motivarían usar MUI están en MUI X **Pro** (pago). Como TanStack Table + PL-3 (plantilla de reporte exportable con Apache POI/OpenPDF del lado del servidor) ya resuelven agrupamiento y exportación sin depender del grid, ese diferencial de MUI no aporta acá.

**Consecuencias**
- F1.4 debe scaffoldear la instalación de Tailwind + `shadcn` CLI y vendorizar el set base de primitivas antes de que arranque cualquier CRUD (bloquea F2.x).
- La plantilla PL-2 (CRUD frontend) se actualiza para referenciar explícitamente los componentes shadcn (`Table`, `Form`, `Dialog`, `Select`, `Badge` para estados borrador/confirmado/anulado) como parte del código molde de F1.8.
- Theming (paleta corporativa, tipografía) se centraliza en `tailwind.config.ts` + variables CSS de shadcn (`:root` con tokens de color) — un solo lugar para cuando llegue el logo/branding (placeholder ya previsto en F1.1 §ADR de parámetros).
- Accesibilidad de base garantizada por Radix (foco, teclado, ARIA) sin trabajo adicional por componente.

---

## 4. Estructura de repo resultante (para F1.3/F1.4)

```
/ (raíz del repo del sistema — no confundir con el repo del plan)
├── backend/
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd
│   └── src/main/java/com/montanaritech/contable/...   (paquetes por dominio, ADR-01 de F1.1)
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   └── src/
│       ├── components/ui/   (primitivas shadcn vendorizadas)
│       └── ...
├── docker/
│   └── docker-compose.yml   (o en la raíz, a definir en F1.3)
└── .github/workflows/       (F1.7)
```

> Nota: esta es la estructura del **repositorio del sistema a construir**, distinta de la carpeta `Montanari Tech Accountability` (plan/inputs/outputs) usada para orquestar el desarrollo. F1.3 debe indicar dónde vive ese repo del sistema respecto de esta carpeta.

---

## 5. Qué confirmar en el checkpoint (rápido, con el equipo)

1. Nombre definitivo del repo del sistema y su ubicación (¿separado de esta carpeta de orquestación, o subcarpeta de ella?).
2. Conformidad con shadcn/ui + Tailwind — implica adoptar Tailwind como convención de estilos en todo el frontend (sin CSS Modules ni styled-components mezclados).
3. Conformidad con Maven — implica que todo desarrollador/modelo use `mvnw` en vez de una instalación local de Gradle.
