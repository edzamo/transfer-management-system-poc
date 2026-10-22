export type TipoCuenta = 'AHORRO' | 'CORRIENTE';
export type EstadoCuenta = 'ACTIVA' | 'INACTIVA';

export interface Cuenta {
  id: number;
  numero: string;
  titular: string;
  tipo: TipoCuenta;
  saldo: number;
  estado: EstadoCuenta;
}
