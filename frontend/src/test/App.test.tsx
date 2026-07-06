import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it } from "vitest"

import App from "@/App"
import { authToken } from "@/lib/auth-token"

/**
 * Smoke test de F1.4/F1.5: layout navegable + wiring de RHF/Zod + el guard
 * de rutas (RequireAuth) funcionando de punta a punta en jsdom. Se simula
 * un token ya presente para no quedar en /login (eso lo cubre login.test.tsx).
 */
describe("App", () => {
  beforeEach(() => {
    authToken.setTokens("fake-access-token", "fake-refresh-token")
  })

  afterEach(() => {
    authToken.clear()
  })

  it("renderiza el layout con la navegación de los módulos", async () => {
    render(<App />)

    expect(
      await screen.findByRole("heading", { name: /dashboard/i })
    ).toBeInTheDocument()

    expect(screen.getByRole("link", { name: /contabilidad/i })).toBeInTheDocument()
    expect(
      screen.getByRole("link", { name: /facturación, cobros y pagos/i })
    ).toBeInTheDocument()
  })

  it("valida el formulario de ejemplo con Zod antes de enviar", async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      await screen.findByRole("link", { name: /ejemplo de formulario/i })
    )
    await user.click(screen.getByRole("button", { name: /guardar/i }))

    expect(
      await screen.findByText(/el nombre comercial es obligatorio/i)
    ).toBeInTheDocument()
  })
})
