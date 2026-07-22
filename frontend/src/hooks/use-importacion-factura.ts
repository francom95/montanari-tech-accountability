import { useMutation, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { FilaImportacionConfirmarInput, FilaImportacionPreview, FilaImportacionResultado } from "@/types/importacion-factura"

export function usePrevisualizarFacturasPdf() {
  return useMutation({
    mutationFn: async (archivos: File[]) => {
      const formData = new FormData()
      archivos.forEach((archivo) => formData.append("archivos", archivo))
      return (
        await http.post<FilaImportacionPreview[]>("/importacion-facturas/pdf/previsualizar", formData, {
          headers: { "Content-Type": "multipart/form-data" },
        })
      ).data
    },
  })
}

export function useConfirmarImportacionFacturas() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (filas: FilaImportacionConfirmarInput[]) =>
      (await http.post<FilaImportacionResultado[]>("/importacion-facturas/confirmar", filas)).data,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["factura-venta"] })
      await qc.invalidateQueries({ queryKey: ["factura-compra"] })
      await qc.invalidateQueries({ queryKey: ["cliente"] })
      await qc.invalidateQueries({ queryKey: ["proveedor"] })
    },
  })
}

export async function descargarRechazosImportacionExcel(rechazos: FilaImportacionResultado[]) {
  const respuesta = await http.post("/importacion-facturas/rechazos/exportar/excel", rechazos, { responseType: "blob" })
  const url = window.URL.createObjectURL(respuesta.data)
  const enlace = document.createElement("a")
  enlace.href = url
  enlace.download = "rechazos-importacion.xlsx"
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  window.URL.revokeObjectURL(url)
}
