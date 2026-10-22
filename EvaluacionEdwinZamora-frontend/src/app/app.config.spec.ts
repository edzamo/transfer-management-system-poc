import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { appConfig } from './app.config';
import { CuentaRepository } from './domain/ports/cuenta.repository';
import { TransferenciaRepository } from './domain/ports/transferencia.repository';
import { HttpCuentaRepository } from './infrastructure/http/http-cuenta.repository';
import { HttpTransferenciaRepository } from './infrastructure/http/http-transferencia.repository';

describe('appConfig (composition root)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
  });

  it('debe_enlazar_los_puertos_con_sus_adaptadores_http', () => {
    expect(TestBed.inject(CuentaRepository)).toBeInstanceOf(HttpCuentaRepository);
    expect(TestBed.inject(TransferenciaRepository)).toBeInstanceOf(HttpTransferenciaRepository);
  });
});
