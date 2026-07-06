import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, describe, expect, it } from "vitest"

import App from "@/App"
import { authToken } from "@/lib/auth-token"

describe("RequireAuth + LoginPage", () => {
  afterEach(() => {
    authToken.clear()
  })

  it("redirige a /login cuando no hay token", async () => {
    render(<App />)

    expect(await screen.findByText(/montanari tech/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/contraseña/i)).toBeInTheDocument()
  })

  it("valida el formulario de login antes de enviar", async () => {
    const user = userEvent.setup()
    render(<App />)

    await screen.findByText(/montanari tech/i)
    await user.click(screen.getByRole("button", { name: /ingresar/i }))

    expect(await screen.findByText(/email inválido/i)).toBeInTheDocument()
  })
})
