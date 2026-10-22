import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ErrorDeRed } from '../../domain/errors/error-de-red.error';
import { Cuenta } from '../../domain/models/cuenta';
import { Page } from '../../domain/models/page';
import { HttpCuentaRepository } from './http-cuenta.repository';

describe('HttpCuentaRepository', () => {
  let repositorio: HttpCuentaRepository;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    repositorio = TestBed.inject(HttpCuentaRepository);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('debe_hacer_GET_a_api_cuentas_con_page_y_size_y_devolver_la_pagina', async () => {
    const cuenta: Cuenta = {
      id: 1,
      numero: '1000000001',
      titular: 'Ana',
      tipo: 'AHORRO',
      saldo: 250.75,
      estado: 'ACTIVA',
    };
    const pagina: Page<Cuenta> = {
      content: [cuenta],
      page: 0,
      size: 200,
      totalElements: 1,
      totalPages: 1,
    };

    const resultado = firstValueFrom(repositorio.listar(0, 200));
    const peticion = http.expectOne((r) => r.url === '/api/cuentas');
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.params.get('page')).toBe('0');
    expect(peticion.request.params.get('size')).toBe('200');
    peticion.flush(pagina);

    expect(await resultado).toEqual(pagina);
  });

  it('debe_fallar_con_ErrorDeRed_cuando_el_servidor_no_responde', async () => {
    const resultado = firstValueFrom(repositorio.listar(0, 200));
    http.expectOne((r) => r.url === '/api/cuentas').error(new ProgressEvent('error'));

    await expect(resultado).rejects.toBeInstanceOf(ErrorDeRed);
  });
});
