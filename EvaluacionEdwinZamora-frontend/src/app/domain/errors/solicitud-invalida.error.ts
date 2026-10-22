import { ErrorValidacion } from '../rules/validar-transferencia';

export class SolicitudInvalidaError extends Error {
  constructor(readonly errores: ErrorValidacion[]) {
    super('La solicitud de transferencia no es valida');
    this.name = 'SolicitudInvalidaError';
  }
}
