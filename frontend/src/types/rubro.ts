export type Rubro = { id: number; nombre: string; categoriaId: number; orden: number; activo: boolean }
export type RubroCrearInput = { nombre: string; categoriaId: number; orden: number }
export type RubroEditarInput = { nombre: string; categoriaId: number; orden: number }
