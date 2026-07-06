import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { RequireAdmin } from "@/components/require-admin"
import { http } from "@/lib/http"
import type { PageResponse, RolUsuario, Usuario } from "@/types/auth"

const ROLES: RolUsuario[] = ["ADMINISTRADOR", "CARGA", "LECTURA"]

const esquemaCrearUsuario = z.object({
  email: z.string().email("Email inválido"),
  nombre: z.string().min(1, "El nombre es obligatorio"),
  password: z.string().min(8, "Mínimo 8 caracteres"),
  rol: z.enum(["ADMINISTRADOR", "CARGA", "LECTURA"]),
})

type CrearUsuarioValores = z.infer<typeof esquemaCrearUsuario>

const USUARIOS_QUERY_KEY = ["usuarios"]

export function UsuariosPage() {
  return (
    <RequireAdmin>
      <GestionUsuarios />
    </RequireAdmin>
  )
}

function GestionUsuarios() {
  const queryClient = useQueryClient()

  const usuariosQuery = useQuery({
    queryKey: USUARIOS_QUERY_KEY,
    queryFn: async () =>
      (await http.get<PageResponse<Usuario>>("/usuarios", { params: { size: 50 } })).data,
  })

  const crearForm = useForm<CrearUsuarioValores>({
    resolver: zodResolver(esquemaCrearUsuario),
    defaultValues: { email: "", nombre: "", password: "", rol: "LECTURA" },
  })

  const crearMutation = useMutation({
    mutationFn: async (valores: CrearUsuarioValores) =>
      (await http.post<Usuario>("/usuarios", valores)).data,
    onSuccess: async () => {
      crearForm.reset()
      await queryClient.invalidateQueries({ queryKey: USUARIOS_QUERY_KEY })
    },
  })

  const cambiarEstadoMutation = useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<Usuario>(`/usuarios/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: USUARIOS_QUERY_KEY })
    },
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Usuarios</h1>
        <p className="text-sm text-muted-foreground">
          Alta y baja de usuarios. Solo administradores acceden a esta pantalla (funcional §14.1).
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Nuevo usuario</CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...crearForm}>
            <form
              onSubmit={crearForm.handleSubmit((valores) => crearMutation.mutate(valores))}
              className="grid gap-4 sm:grid-cols-2"
            >
              <FormField
                control={crearForm.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={crearForm.control}
                name="nombre"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nombre</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={crearForm.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Contraseña</FormLabel>
                    <FormControl>
                      <Input type="password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={crearForm.control}
                name="rol"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Rol</FormLabel>
                    <FormControl>
                      <select
                        {...field}
                        className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                      >
                        {ROLES.map((rol) => (
                          <option key={rol} value={rol}>
                            {rol}
                          </option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {crearMutation.isError && (
                <p className="text-sm text-destructive sm:col-span-2">
                  No se pudo crear el usuario (¿el email ya existe?).
                </p>
              )}

              <Button type="submit" disabled={crearMutation.isPending} className="sm:col-span-2">
                {crearMutation.isPending ? "Creando…" : "Crear usuario"}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Listado</CardTitle>
        </CardHeader>
        <CardContent>
          {usuariosQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Email</th>
                  <th className="py-2 pr-4 font-medium">Nombre</th>
                  <th className="py-2 pr-4 font-medium">Rol</th>
                  <th className="py-2 pr-4 font-medium">Estado</th>
                  <th className="py-2 font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {usuariosQuery.data?.content.map((usuario) => (
                  <tr key={usuario.id} className="border-b border-border last:border-0">
                    <td className="py-2 pr-4">{usuario.email}</td>
                    <td className="py-2 pr-4">{usuario.nombre}</td>
                    <td className="py-2 pr-4">{usuario.rol}</td>
                    <td className="py-2 pr-4">
                      {usuario.activo ? "Activo" : "Inactivo"}
                    </td>
                    <td className="py-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={cambiarEstadoMutation.isPending}
                        onClick={() =>
                          cambiarEstadoMutation.mutate({ id: usuario.id, activo: usuario.activo })
                        }
                      >
                        {usuario.activo ? "Desactivar" : "Activar"}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
