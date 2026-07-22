import { useMutation, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type {
  FilaImportacionBancariaConfirmarInput,
  FilaImportacionBancariaPreview,
  FilaImportacionBancariaResultado,
  OrigenConParser,
} from "@/types/importacion-bancaria"

export function usePrevisualizarResumenBancario() {
  return useMutation({
    mutationFn: async ({ origen, cuentaBancariaId, archivo }: { origen: OrigenConParser; cuentaBancariaId: number; archivo: File }) => {
      const formData = new FormData()
      formData.append("archivo", archivo)
      return (
        await http.post<FilaImportacionBancariaPreview[]>(
          `/importacion-movimientos-bancarios/${origen}/previsualizar`,
          formData,
          { params: { cuentaBancariaId }, headers: { "Content-Type": "multipart/form-data" } }
        )
      ).data
    },
  })
}

export function useConfirmarImportacionBancaria() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ origen, cuentaBancariaId, tipoCambioUsd, filas }: {
      origen: OrigenConParser; cuentaBancariaId: number; tipoCambioUsd?: number; filas: FilaImportacionBancariaConfirmarInput[]
    }) =>
      (
        await http.post<FilaImportacionBancariaResultado[]>(
          `/importacion-movimientos-bancarios/${origen}/confirmar`,
          filas,
          { params: { cuentaBancariaId, tipoCambioUsd } }
        )
      ).data,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["movimiento-bancario"] })
    },
  })
}
