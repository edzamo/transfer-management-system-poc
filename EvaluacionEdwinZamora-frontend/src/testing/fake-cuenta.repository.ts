import { NEVER, Observable, of, throwError } from 'rxjs';
import { Cuenta } from '../app/domain/models/cuenta';
import { ErrorDeApi } from '../app/domain/errors/error-de-api.error';
import { Page, TAMANO_PAGINA_MAXIMO } from '../app/domain/models/page';
import { CuentaRepository } from '../app/domain/ports/cuenta.repository';

/** Fake en memoria del puerto CuentaRepository (solo para tests). */
export class FakeCuentaRepository extends CuentaRepository {
  readonly llamadas: { page: number; size: number }[] = [];

  constructor(
    private readonly cuentas: Cuenta[] = [],
    private readonly comportamiento: Error | 'pendiente' | null = null,
  ) {
    super();
  }

  listar(page: number, size: number): Observable<Page<Cuenta>> {
    this.llamadas.push({ page, size });
    if (size < 1 || size > TAMANO_PAGINA_MAXIMO) {
      return throwError(
        () =>
          new ErrorDeApi({
            status: 400,
            code: 'PARAMETRO_INVALIDO',
            message: `size debe estar entre 1 y ${TAMANO_PAGINA_MAXIMO}`,
          }),
      );
    }
    if (this.comportamiento === 'pendiente') {
      return NEVER;
    }
    if (this.comportamiento) {
      return throwError(() => this.comportamiento);
    }
    const inicio = page * size;
    return of({
      content: this.cuentas.slice(inicio, inicio + size),
      page,
      size,
      totalElements: this.cuentas.length,
      totalPages: Math.ceil(this.cuentas.length / size),
    });
  }
}
