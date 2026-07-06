export type Jurisdiccion = { id: number; nombre: string; codigo: string; alicuotaIIBB: string; activo: boolean }
export type JurisdiccionCrearInput = { nombre: string; codigo: string; alicuotaIIBB: string }
export type JurisdiccionEditarInput = { nombre: string; alicuotaIIBB: string }
