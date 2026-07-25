export type TipoAlerta =
  | "VENCIMIENTO_PROXIMO"
  | "VENCIMIENTO_VENCIDO"
  | "COMPROMISO_PROXIMO"
  | "CXP_PROXIMO"
  | "CXC_ATRASADA"
  | "SALDO_BAJO"
  | "MOVIMIENTO_BANCARIO_PENDIENTE"
  | "CONCILIACION_DIFERENCIA"
  | "PENDIENTE_ADMINISTRATIVO_PROXIMO"

export type SeveridadAlerta = "INFO" | "ADVERTENCIA" | "CRITICA"
export type EstadoAlerta = "ACTIVA" | "RESUELTA"

export type Alerta = {
  id: number
  tipo: TipoAlerta
  severidad: SeveridadAlerta
  mensaje: string
  entidadTipo: string
  entidadRefId: number
  fecha: string
  estado: EstadoAlerta
  leida: boolean
}
