export type EstadoTransferencia = 'COMPLETADA' | 'EN_REVISION';

export interface SolicitudTransferencia {
  cuentaOrigen: string;
  cuentaDestino: string;
  monto: number;
}

export interface Transferencia {
  id: number;
  cuentaOrigen: string;
  cuentaDestino: string;
  monto: number;
  estado: EstadoTransferencia;
  motivo: string | null;
  fecha: string;
}
