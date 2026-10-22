import { Cuenta } from '../models/cuenta';

export type OrdenSaldo = 'asc' | 'desc' | null;

/** Ordena por saldo en el cliente; devuelve una copia y nunca muta la entrada. */
export function ordenarPorSaldo(cuentas: readonly Cuenta[], orden: OrdenSaldo): Cuenta[] {
  const copia = [...cuentas];
  if (orden === null) {
    return copia;
  }
  const signo = orden === 'asc' ? 1 : -1;
  return copia.sort((a, b) => (a.saldo - b.saldo) * signo);
}
