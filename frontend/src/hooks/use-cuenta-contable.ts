import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { CuentaContable, CuentaContableCrearInput, CuentaContableEditarInput, CuentaContableNodo } from "@/types/cuenta-contable"

const QUERY_KEY = ["cuenta-contable"]

export function useCuentasContables(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<CuentaContable>>("/cuentas-contables", {
          params: { texto: params.texto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 10 },
        })
      ).data,
  })
}

export function useArbolCuentasContables() {
  return useQuery({
    queryKey: [...QUERY_KEY, "arbol"],
    queryFn: async () => (await http.get<CuentaContableNodo[]>("/cuentas-contables/arbol")).data,
  })
}

export function useCrearCuentaContable() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: CuentaContableCrearInput) => (await http.post<CuentaContable>("/cuentas-contables", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarCuentaContable() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: CuentaContableEditarInput }) =>
      (await http.put<CuentaContable>(`/cuentas-contables/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoCuentaContable() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<CuentaContable>(`/cuentas-contables/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarCuentaContable() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/cuentas-contables/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}
