import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SolicitudTransferencia, Transferencia } from '../../domain/models/transferencia';
import { TransferenciaRepository } from '../../domain/ports/transferencia.repository';
import { API_BASE_URL } from '../config/api.config';
import { traducirErroresHttp } from './translate-http-error';

@Injectable({ providedIn: 'root' })
export class HttpTransferenciaRepository extends TransferenciaRepository {
  private readonly http = inject(HttpClient);

  crear(solicitud: SolicitudTransferencia): Observable<Transferencia> {
    return this.http
      .post<Transferencia>(`${API_BASE_URL}/transferencias`, solicitud)
      .pipe(traducirErroresHttp());
  }
}
