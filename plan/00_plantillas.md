# 00 — Plantillas reutilizables (PL-1 a PL-5)

> **Adjuntar este archivo en toda sesión de Haiku 4.5 y en las de Sonnet 5 que referencien plantillas.**
> El código molde real vive en el paso F1.8; este archivo es la definición conceptual.

## PL-1 — CRUD Backend
Entity JPA (con `tenant_id`, `activo`, timestamps de auditoría) + Repository + Service + Controller REST + DTOs request/response + MapStruct + Bean Validation + migración Flyway propia. Operaciones: listar paginado con filtros, buscar, crear, editar, activar/desactivar (soft-delete), eliminar solo si no hay movimientos asociados (HTTP 409 con mensaje claro). Endpoints documentados en OpenAPI. Tests: unitario de service + integración Testcontainers del happy path.

## PL-2 — CRUD Frontend
Ruta + listado TanStack Table (paginación, orden, filtros, búsqueda) + formulario React Hook Form + esquema Zod espejando validaciones del backend + hooks React Query (list/get/create/update/toggle/delete) + manejo de 409 + estados de carga. Tipos TypeScript espejados del contrato OpenAPI.

## PL-3 — Reporte exportable
Endpoint de consulta con filtros estándar (período desde/hasta, proyecto, cuenta, cliente, proveedor, moneda según aplique) + servicio de agregación + vista con TanStack Table + botones "Exportar Excel" (Apache POI) y "Exportar PDF" (OpenPDF) usando el `ReportExportService` común con estilos corporativos (placeholder de logo hasta recibirlo). Export en streaming para volúmenes grandes.

## PL-4 — Asiento automático
Interfaz `AsientoGenerator` por tipo de evento (factura venta, factura compra, cobro, pago, IVA, IIBB, resumen tarjeta): evento confirmado → construcción de líneas debe/haber según reglas parametrizadas en la tabla de mapeo concepto→cuenta (editable por admin) → validación Σdebe = Σhaber (si no balancea: excepción, jamás persiste confirmado) → numeración interna automática compartida por todas las líneas → vínculo bidireccional al documento origen → editable post-generación (solo roles permitidos) → todo cambio en auditoría.

## PL-5 — Máquina de estados
Enum `borrador / confirmado / anulado` + transiciones válidas: borrador→confirmado, confirmado→anulado; anulado es terminal. Reglas: solo `confirmado` impacta contabilidad y reportes; `anulado` conserva trazabilidad y revierte o marca el asiento vinculado según la regla de anulación de F3.1. Cambios de estado auditados. Aplica a facturas, cobros, pagos y asientos.
