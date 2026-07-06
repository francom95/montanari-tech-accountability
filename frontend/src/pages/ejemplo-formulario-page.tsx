import { zodResolver } from "@hookform/resolvers/zod"
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

/**
 * Ejemplo mínimo del patrón RHF + Zod que van a replicar los formularios
 * reales (PL-2, formalizado en F1.8): esquema Zod como única fuente de
 * verdad de la validación, espejado en el tipo del formulario.
 */
const esquemaClienteEjemplo = z.object({
  nombreComercial: z.string().min(1, "El nombre comercial es obligatorio"),
  cuit: z
    .string()
    .regex(/^\d{2}-\d{8}-\d{1}$/, "Formato esperado: 30-12345678-9"),
  email: z.string().email("Email inválido").optional().or(z.literal("")),
})

type ClienteEjemplo = z.infer<typeof esquemaClienteEjemplo>

export function EjemploFormularioPage() {
  const form = useForm<ClienteEjemplo>({
    resolver: zodResolver(esquemaClienteEjemplo),
    defaultValues: { nombreComercial: "", cuit: "", email: "" },
  })

  function onSubmit(valores: ClienteEjemplo) {
    // Placeholder: F2.2 (CRUD Clientes) reemplaza esto por la mutación real
    // de React Query contra el backend.
    console.log("Formulario válido:", valores)
  }

  return (
    <Card className="max-w-md">
      <CardHeader>
        <CardTitle>Ejemplo de formulario tipado (F1.4)</CardTitle>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="nombreComercial"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre comercial</FormLabel>
                  <FormControl>
                    <Input placeholder="Montanari Tech" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="cuit"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>CUIT</FormLabel>
                  <FormControl>
                    <Input placeholder="30-12345678-9" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input type="email" placeholder="contacto@cliente.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <Button type="submit">Guardar</Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  )
}
