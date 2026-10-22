export interface BorradorTransferencia {
  cuentaOrigen: string | null;
  cuentaDestino: string | null;
  monto: number | string | null;
}

export type CodigoValidacion = 'REQUERIDO' | 'MONTO_INVALIDO' | 'MISMA_CUENTA';

export interface ErrorValidacion {
  campo: keyof BorradorTransferencia;
  codigo: CodigoValidacion;
}

const CAMPOS_REQUERIDOS: (keyof BorradorTransferencia)[] = [
  'cuentaOrigen',
  'cuentaDestino',
  'monto',
];

const estaVacio = (valor: string | number | null): boolean => valor === null || valor === '';

const esMontoPositivo = (monto: string | number): boolean =>
  Number.isFinite(Number(monto)) && Number(monto) > 0;

/** Validacion de UX (el backend es la fuente de verdad de saldo, estado y limites). */
export function validarTransferencia(borrador: BorradorTransferencia): ErrorValidacion[] {
  const errores: ErrorValidacion[] = CAMPOS_REQUERIDOS.filter((campo) =>
    estaVacio(borrador[campo]),
  ).map((campo) => ({ campo, codigo: 'REQUERIDO' }));
  if (borrador.monto !== null && !estaVacio(borrador.monto) && !esMontoPositivo(borrador.monto)) {
    errores.push({ campo: 'monto', codigo: 'MONTO_INVALIDO' });
  }
  if (!estaVacio(borrador.cuentaOrigen) && borrador.cuentaOrigen === borrador.cuentaDestino) {
    errores.push({ campo: 'cuentaDestino', codigo: 'MISMA_CUENTA' });
  }
  return errores;
}
