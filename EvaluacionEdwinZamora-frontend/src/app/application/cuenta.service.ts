import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Cuenta } from '../domain/models/cuenta';
import { Page } from '../domain/models/page';
import { CuentaRepository } from '../domain/ports/cuenta.repository';

@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly repositorio = inject(CuentaRepository);

  listar(page: number, size: number): Observable<Page<Cuenta>> {
    return this.repositorio.listar(page, size);
  }
}
