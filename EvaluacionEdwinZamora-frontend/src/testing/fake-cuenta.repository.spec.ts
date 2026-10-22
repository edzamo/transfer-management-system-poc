import { firstValueFrom } from 'rxjs';
import { ErrorDeApi } from '../app/domain/errors/error-de-api.error';
import { TAMANO_PAGINA_MAXIMO } from '../app/domain/models/page';
import { FakeCuentaRepository } from './fake-cuenta.repository';

describe('FakeCuentaRepository (contrato de tamano de pagina del backend)', () => {
  it('debe_aceptar_el_tamano_maximo_de_pagina', async () => {
    const pagina = await firstValueFrom(new FakeCuentaRepository().listar(0, TAMANO_PAGINA_MAXIMO));

    expect(pagina.size).toBe(TAMANO_PAGINA_MAXIMO);
  });

  it.each([0, TAMANO_PAGINA_MAXIMO + 1])(
    'debe_fallar_con_400_cuando_el_tamano_es_%i',
    async (size) => {
      const resultado = firstValueFrom(new FakeCuentaRepository().listar(0, size));

      await expect(resultado).rejects.toBeInstanceOf(ErrorDeApi);
      await expect(resultado).rejects.toMatchObject({ status: 400 });
    },
  );
});
