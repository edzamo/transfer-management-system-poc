import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ErrorDeRed } from '../../domain/errors/error-de-red.error';
import { ErrorDeApi } from '../../domain/errors/error-de-api.error';
import { SolicitudTransferencia, Transferencia } from '../../domain/models/transferencia';
import { HttpTransferenciaRepository } from './http-transferencia.repository';

const solicitud: SolicitudTransferencia = {
  cuentaOrigen: '1000000001',
  cuentaDestino: '1000000002',
  monto: 5500,
};

describe('HttpTransferenciaRepository', () => {
  let repositorio: HttpTransferenciaRepository;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    repositorio = TestBed.inject(HttpTransferenciaRepository);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('debe_hacer_POST_a_api_transferencias_con_el_cuerpo_y_devolver_la_transferencia', async () => {
    const creada: Transferencia = {
      id: 7,
      ...solicitud,
      estado: 'EN_REVISION',
      motivo: 'Monto superior a 5000',
      fecha: '2026-10-22T10:00:00Z',
    };

    const resultado = firstValueFrom(repositorio.crear(solicitud));
    const peticion = http.expectOne('/api/transferencias');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(solicitud);
    peticion.flush(creada, { status: 201, statusText: 'Created' });

    expect(await resultado).toEqual(creada);
  });

  it.each([400, 404, 422])(
    'debe_fallar_con_ErrorDeApi_cuando_el_backend_responde_%i',
    async (status) => {
      const resultado = firstValueFrom(repositorio.crear(solicitud));
      http.expectOne('/api/transferencias').flush(
        {
          status,
          code: 'CODIGO',
          message: 'mensaje del backend',
          details: [{ field: 'monto', message: 'x' }],
        },
        { status, statusText: 'Error' },
      );

      await expect(resultado).rejects.toBeInstanceOf(ErrorDeApi);
      await expect(resultado).rejects.toMatchObject({ status, message: 'mensaje del backend' });
    },
  );

  it('debe_fallar_con_ErrorDeRed_cuando_el_servidor_no_responde', async () => {
    const resultado = firstValueFrom(repositorio.crear(solicitud));
    http.expectOne('/api/transferencias').error(new ProgressEvent('error'));

    await expect(resultado).rejects.toBeInstanceOf(ErrorDeRed);
  });
});
