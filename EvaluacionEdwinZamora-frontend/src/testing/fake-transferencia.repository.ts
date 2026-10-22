import { NEVER, Observable, of, throwError } from 'rxjs';
import { SolicitudTransferencia, Transferencia } from '../app/domain/models/transferencia';
import { TransferenciaRepository } from '../app/domain/ports/transferencia.repository';

export interface OpcionesFakeTransferencia {
  fallo?: Error;
  estado?: Transferencia['estado'];
  motivo?: string | null;
  pendiente?: boolean;
}

/** Fake en memoria del puerto TransferenciaRepository (solo para tests). */
export class FakeTransferenciaRepository extends TransferenciaRepository {
  readonly solicitudes: SolicitudTransferencia[] = [];
  private siguienteId = 1;

  constructor(private readonly opciones: OpcionesFakeTransferencia = {}) {
    super();
  }

  crear(solicitud: SolicitudTransferencia): Observable<Transferencia> {
    this.solicitudes.push(solicitud);
    if (this.opciones.pendiente) {
      return NEVER;
    }
    if (this.opciones.fallo) {
      return throwError(() => this.opciones.fallo);
    }
    return of({
      id: this.siguienteId++,
      ...solicitud,
      estado: this.opciones.estado ?? 'COMPLETADA',
      motivo: this.opciones.motivo ?? null,
      fecha: '2026-10-22T10:00:00Z',
    });
  }
}
