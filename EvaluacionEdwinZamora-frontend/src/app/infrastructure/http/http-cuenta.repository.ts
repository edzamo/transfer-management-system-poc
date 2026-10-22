import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Cuenta } from '../../domain/models/cuenta';
import { Page } from '../../domain/models/page';
import { CuentaRepository } from '../../domain/ports/cuenta.repository';
import { API_BASE_URL } from '../config/api.config';
import { traducirErroresHttp } from './translate-http-error';

@Injectable({ providedIn: 'root' })
export class HttpCuentaRepository extends CuentaRepository {
  private readonly http = inject(HttpClient);

  listar(page: number, size: number): Observable<Page<Cuenta>> {
    return this.http
      .get<Page<Cuenta>>(`${API_BASE_URL}/cuentas`, { params: { page, size } })
      .pipe(traducirErroresHttp());
  }
}
