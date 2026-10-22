export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Tamaño máximo de página que acepta el backend (size 1..200). */
export const TAMANO_PAGINA_MAXIMO = 200;
