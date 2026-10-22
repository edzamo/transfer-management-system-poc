import { Observable } from 'rxjs';
import { Cuenta } from '../models/cuenta';
import { Page } from '../models/page';

/** Puerto de salida; la clase abstracta sirve de token de inyeccion. */
export abstract class CuentaRepository {
  abstract listar(page: number, size: number): Observable<Page<Cuenta>>;
}
