import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export function DashboardPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          Ventas, cobros, cuentas por cobrar/pagar, saldo de caja y alertas — se completa en F7.5.
        </p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Layout de F1.4</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Esta pantalla confirma que el layout (sidebar + header + contenido), el ruteo y el
          proveedor de React Query están funcionando de punta a punta.
        </CardContent>
      </Card>
    </div>
  )
}
