import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { CuentaBancaria, CuentaBancariaCrearInput, CuentaBancariaEditarInput } from "@/types/cuenta-bancaria"

const QUERY_KEY = ["cuentaBancaria"]

export function useCuentasBancarias(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () => (await http.get<PageResponse<CuentaBancaria>>("/cuentas-bancarias", { params: { texto: params.texto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 10 } })).data,
  })
}

export function useCrearCuentaBancaria() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: CuentaBancariaCrearInput) => (await http.post<CuentaBancaria>("/cuentas-bancarias", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarCuentaBancaria() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: CuentaBancariaEditarInput }) => (await http.put<CuentaBancaria>(`/cuentas-bancarias/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoCuentaBancaria() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) => (await http.patch<CuentaBancaria>(`/cuentas-bancarias/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarCuentaBancaria() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/cuentas-bancarias/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}
