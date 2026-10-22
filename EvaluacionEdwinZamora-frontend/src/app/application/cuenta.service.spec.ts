import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { FakeCuentaRepository } from '../../testing/fake-cuenta.repository';
import { Cuenta } from '../domain/models/cuenta';
import { CuentaRepository } from '../domain/ports/cuenta.repository';
import { CuentaService } from './cuenta.service';

const cuenta = (id: number, saldo: number): Cuenta => ({
  id,
  numero: `100000000${id}`,
  titular: `Titular ${id}`,
  tipo: 'AHORRO',
  saldo,
  estado: 'ACTIVA',
});

describe('CuentaService', () => {
  let repositorio: FakeCuentaRepository;
  let servicio: CuentaService;

  beforeEach(() => {
    repositorio = new FakeCuentaRepository([cuenta(1, 500), cuenta(2, 100), cuenta(3, 900)]);
    TestBed.configureTestingModule({
      providers: [{ provide: CuentaRepository, useValue: repositorio }],
    });
    servicio = TestBed.inject(CuentaService);
  });

  it('debe_devolver_la_pagina_de_cuentas_del_repositorio_cuando_se_listan', async () => {
    const pagina = await firstValueFrom(servicio.listar(0, 2));

    expect(pagina.content.map((c) => c.id)).toEqual([1, 2]);
    expect(pagina.totalElements).toBe(3);
    expect(repositorio.llamadas).toEqual([{ page: 0, size: 2 }]);
  });
});
