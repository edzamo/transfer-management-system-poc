import { Observable } from 'rxjs';
import { SolicitudTransferencia, Transferencia } from '../models/transferencia';

/** Puerto de salida; la clase abstracta sirve de token de inyeccion. */
export abstract class TransferenciaRepository {
  abstract crear(solicitud: SolicitudTransferencia): Observable<Transferencia>;
}
