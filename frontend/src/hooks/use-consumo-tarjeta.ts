import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  ClasificarConsumoInput,
  ConsumoImportacionConfirmarInput,
  ConsumoImportacionPreview,
  ConsumoImportacionResultado,
  ConsumoTarjeta,
  PagoTarjeta,
  PagoTarjetaCrearInput,
  ReglaClasificacionConsumo,
  ReglaClasificacionConsumoInput,
} from "@/types/consumo-tarjeta"

const CONSUMO_KEY = ["consumoTarjeta"]
const PAGO_KEY = ["pagoTarjeta"]
const REGLA_KEY = ["reglaClasificacionConsumo"]

export function useConsumosTarjeta(params: { tarjetaCreditoId?: number; soloSinClasificar?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...CONSUMO_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<ConsumoTarjeta>>("/consumos-tarjeta", {
          params: {
            tarjetaCreditoId: params.tarjetaCreditoId,
            soloSinClasificar: params.soloSinClasificar,
            page: params.page ?? 0,
            size: params.size ?? 20,
          },
        })
      ).data,
    enabled: !!params.tarjetaCreditoId,
  })
}

export function useContadorSinClasificar(tarjetaCreditoId?: number) {
  return useQuery({
    queryKey: [...CONSUMO_KEY, "contador", tarjetaCreditoId],
    queryFn: async () => (await http.get<number>("/consumos-tarjeta/sin-clasificar/contador", { params: { tarjetaCreditoId } })).data,
    enabled: !!tarjetaCreditoId,
  })
}

export function useClasificarConsumo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: ClasificarConsumoInput }) =>
      (await http.patch<ConsumoTarjeta>(`/consumos-tarjeta/${id}/clasificar`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: CONSUMO_KEY }) },
  })
}

export function useClasificarMasivamente() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (tarjetaCreditoId: number) =>
      (await http.post<number>("/consumos-tarjeta/clasificar-masivamente", null, { params: { tarjetaCreditoId } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: CONSUMO_KEY }) },
  })
}

export function usePrevisualizarConsumosTarjeta() {
  return useMutation({
    mutationFn: async ({ tarjetaCreditoId, archivo }: { tarjetaCreditoId: number; archivo: File }) => {
      const formData = new FormData()
      formData.append("archivo", archivo)
      return (
        await http.post<ConsumoImportacionPreview[]>(
          `/tarjetas-credito/${tarjetaCreditoId}/importacion-consumos/previsualizar`,
          formData,
          { headers: { "Content-Type": "multipart/form-data" } }
        )
      ).data
    },
  })
}

export function useConfirmarImportacionConsumos() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ tarjetaCreditoId, tipoCambioUsd, filas }: {
      tarjetaCreditoId: number; tipoCambioUsd?: number; filas: ConsumoImportacionConfirmarInput[]
    }) =>
      (
        await http.post<ConsumoImportacionResultado[]>(
          `/tarjetas-credito/${tarjetaCreditoId}/importacion-consumos/confirmar`,
          filas,
          { params: { tipoCambioUsd } }
        )
      ).data,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: CONSUMO_KEY })
      await qc.invalidateQueries({ queryKey: ["tarjetaCredito"] })
    },
  })
}

export function usePagosTarjeta(tarjetaCreditoId?: number, page?: number, size?: number) {
  return useQuery({
    queryKey: [...PAGO_KEY, tarjetaCreditoId, page, size],
    queryFn: async () =>
      (await http.get<PageResponse<PagoTarjeta>>("/pagos-tarjeta", { params: { tarjetaCreditoId, page: page ?? 0, size: size ?? 10 } })).data,
    enabled: !!tarjetaCreditoId,
  })
}

export function useCrearPagoTarjeta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: PagoTarjetaCrearInput) => (await http.post<PagoTarjeta>("/pagos-tarjeta", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: PAGO_KEY }) },
  })
}

export function useConfirmarPagoTarjeta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<PagoTarjeta>(`/pagos-tarjeta/${id}/confirmar`)).data,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: PAGO_KEY })
      await qc.invalidateQueries({ queryKey: ["tarjetaCredito"] })
    },
  })
}

export function useAnularPagoTarjeta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<PagoTarjeta>(`/pagos-tarjeta/${id}/anular`, { motivo })).data,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: PAGO_KEY })
      await qc.invalidateQueries({ queryKey: ["tarjetaCredito"] })
    },
  })
}

export function useReglasClasificacionConsumo(activo?: boolean) {
  return useQuery({
    queryKey: [...REGLA_KEY, activo],
    queryFn: async () => (await http.get<PageResponse<ReglaClasificacionConsumo>>("/reglas-clasificacion-consumo", { params: { activo, size: 200 } })).data,
  })
}

export function useCrearReglaClasificacion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ReglaClasificacionConsumoInput) => (await http.post<ReglaClasificacionConsumo>("/reglas-clasificacion-consumo", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: REGLA_KEY }) },
  })
}

export function useCambiarEstadoReglaClasificacion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<ReglaClasificacionConsumo>(`/reglas-clasificacion-consumo/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: REGLA_KEY }) },
  })
}
