export interface DetalleError {
  field: string;
  message: string;
}

export interface DatosErrorApi {
  status: number;
  code: string;
  message: string;
  details?: DetalleError[];
  montoRestante?: number;
}

/** Error HTTP genérico del backend (400/404/409/422...) con el cuerpo de error parseado. */
export class ErrorDeApi extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: DetalleError[];
  readonly montoRestante: number | null;

  constructor(datos: DatosErrorApi) {
    super(datos.message);
    this.name = 'ErrorDeApi';
    this.status = datos.status;
    this.code = datos.code;
    this.details = datos.details ?? [];
    this.montoRestante = datos.montoRestante ?? null;
  }
}
