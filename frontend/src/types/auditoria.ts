export type AccionAuditoria =
  | "CREAR"
  | "EDITAR"
  | "ELIMINAR"
  | "CONFIRMAR"
  | "ANULAR"
  | "DUPLICAR"
  | "CERRAR_PERIODO"
  | "REABRIR_PERIODO"
  | "IMPORTAR"
  | "LOGIN"
  | "CAMBIO_ESTADO"
  | "EXPORTAR_SENSIBLE"

export type AuditoriaLog = {
  id: number
  entidadTipo: string
  entidadId: number
  accion: AccionAuditoria
  usuarioId: number | null
  fechaHora: string
  datosAntes: string | null
  datosDespues: string | null
  sobrePeriodoCerrado: boolean
  detalle: string | null
}
