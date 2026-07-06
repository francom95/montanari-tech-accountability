import { zodResolver } from "@hookform/resolvers/zod"
import { isAxiosError } from "axios"
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
import { useLogin } from "@/hooks/use-auth"

const esquemaLogin = z.object({
  email: z.string().email("Email inválido"),
  password: z.string().min(1, "La contraseña es obligatoria"),
})

type LoginValores = z.infer<typeof esquemaLogin>

export function LoginPage() {
  const login = useLogin()
  const form = useForm<LoginValores>({
    resolver: zodResolver(esquemaLogin),
    defaultValues: { email: "", password: "" },
  })

  function onSubmit(valores: LoginValores) {
    login.mutate(valores)
  }

  return (
    <div className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Montanari Tech</CardTitle>
          <p className="text-sm text-muted-foreground">
            Sistema de Gestión Contable
          </p>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" autoComplete="username" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Contraseña</FormLabel>
                    <FormControl>
                      <Input type="password" autoComplete="current-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {login.isError && (
                <p className="text-sm text-destructive">
                  {isAxiosError(login.error) && login.error.response?.status === 401
                    ? "Email o contraseña incorrectos."
                    : "No se pudo iniciar sesión. Reintentá en un momento."}
                </p>
              )}

              <Button type="submit" className="w-full" disabled={login.isPending}>
                {login.isPending ? "Ingresando…" : "Ingresar"}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
