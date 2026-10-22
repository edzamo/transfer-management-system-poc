import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { CuentaRepository } from './domain/ports/cuenta.repository';
import { TransferenciaRepository } from './domain/ports/transferencia.repository';
import { HttpCuentaRepository } from './infrastructure/http/http-cuenta.repository';
import { HttpTransferenciaRepository } from './infrastructure/http/http-transferencia.repository';

/** Composition root: unico lugar que enlaza puertos de domain con adaptadores de infrastructure. */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(),
    { provide: CuentaRepository, useClass: HttpCuentaRepository },
    { provide: TransferenciaRepository, useClass: HttpTransferenciaRepository },
  ],
};
