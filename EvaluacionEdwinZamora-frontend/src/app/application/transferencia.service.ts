import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { SolicitudInvalidaError } from '../domain/errors/solicitud-invalida.error';
import { Transferencia } from '../domain/models/transferencia';
import { TransferenciaRepository } from '../domain/ports/transferencia.repository';
import { BorradorTransferencia, validarTransferencia } from '../domain/rules/validar-transferencia';

@Injectable({ providedIn: 'root' })
export class TransferenciaService {
  private readonly repositorio = inject(TransferenciaRepository);

  /**
   * Doble validacion deliberada: el componente valida para dar feedback UX inmediato por campo y
   * este servicio revalida como proteccion para cualquier otro consumidor del caso de uso.
   */
  ejecutar(borrador: BorradorTransferencia): Observable<Transferencia> {
    const errores = validarTransferencia(borrador);
    if (errores.length > 0) {
      return throwError(() => new SolicitudInvalidaError(errores));
    }
    return this.repositorio.crear({
      cuentaOrigen: String(borrador.cuentaOrigen),
      cuentaDestino: String(borrador.cuentaDestino),
      monto: Number(borrador.monto),
    });
  }
}
