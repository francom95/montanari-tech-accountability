import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it } from "vitest"

import App from "@/App"
import { authToken } from "@/lib/auth-token"

/** Smoke test de PL-2 (F1.8): la ruta renderiza el formulario y el listado sin explotar. */
describe("MonedasPage", () => {
  beforeEach(() => {
    authToken.setTokens("fake-access-token", "fake-refresh-token")
  })

  afterEach(() => {
    authToken.clear()
  })

  it("navega a /monedas y muestra el formulario de alta", async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole("link", { name: /monedas/i }))

    expect(await screen.findByRole("heading", { name: "Monedas" })).toBeInTheDocument()
    expect(screen.getByRole("button", { name: /crear/i })).toBeInTheDocument()
    expect(screen.getByPlaceholderText("ARS")).toBeInTheDocument()
  })

  it("valida el código de moneda con Zod antes de enviar", async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole("link", { name: /monedas/i }))
    await screen.findByRole("heading", { name: "Monedas" })

    await user.type(screen.getByPlaceholderText("ARS"), "ars")
    await user.click(screen.getByRole("button", { name: /crear/i }))

    expect(await screen.findByText(/código iso 4217/i)).toBeInTheDocument()
  })
})
