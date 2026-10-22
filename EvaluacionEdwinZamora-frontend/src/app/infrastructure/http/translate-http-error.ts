import { HttpErrorResponse } from '@angular/common/http';
import { MonoTypeOperatorFunction, catchError, throwError } from 'rxjs';
import { ErrorDeRed } from '../../domain/errors/error-de-red.error';
import {
  DetalleError,
  ErrorDeApi,
} from '../../domain/errors/error-de-api.error';

interface CuerpoError {
  code: string;
  message: string;
  details?: DetalleError[];
  montoRestante?: number;
}

const esCuerpoError = (cuerpo: unknown): cuerpo is CuerpoError =>
  typeof cuerpo === 'object' &&
  cuerpo !== null &&
  typeof (cuerpo as CuerpoError).code === 'string' &&
  typeof (cuerpo as CuerpoError).message === 'string';

/** Traduce el error HTTP del backend a un error de dominio. */
export function translateHttpError(error: HttpErrorResponse): Error {
  if (error.status === 0) {
    return new ErrorDeRed();
  }
  if (esCuerpoError(error.error)) {
    const { code, message, details, montoRestante } = error.error;
    return new ErrorDeApi({
      status: error.status,
      code,
      message,
      details,
      montoRestante,
    });
  }
  return new ErrorDeApi({
    status: error.status,
    code: 'ERROR_DESCONOCIDO',
    message: `Error inesperado del servidor (${error.status})`,
  });
}

/** Operador rxjs que aplica translateHttpError a los fallos del flujo. */
export const traducirErroresHttp = <T>(): MonoTypeOperatorFunction<T> =>
  catchError((error: HttpErrorResponse) => throwError(() => translateHttpError(error)));
