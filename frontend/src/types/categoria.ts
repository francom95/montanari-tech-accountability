export type Categoria = { id: number; nombre: string; descripcion?: string; tipo: string; activo: boolean }
export type CategoriaCrearInput = { nombre: string; descripcion?: string; tipo: string }
export type CategoriaEditarInput = { nombre: string; descripcion?: string; tipo: string }
