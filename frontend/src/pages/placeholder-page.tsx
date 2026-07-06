import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export function PlaceholderPage({ title }: { title: string }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          Módulo pendiente de implementación — ver <code>./plan</code> para el paso que lo cubre.
        </p>
      </CardContent>
    </Card>
  )
}
