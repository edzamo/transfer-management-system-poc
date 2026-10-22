import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ErrorDeRed } from '../../domain/errors/error-de-red.error';
import { FakeCuentaRepository } from '../../../testing/fake-cuenta.repository';
import { Cuenta } from '../../domain/models/cuenta';
import { TAMANO_PAGINA_MAXIMO } from '../../domain/models/page';
import { CuentaRepository } from '../../domain/ports/cuenta.repository';
import { CuentasComponent } from './cuentas.component';

const cuenta = (id: number, saldo: number, extra: Partial<Cuenta> = {}): Cuenta => ({
  id,
  numero: `100000000${id}`,
  titular: `Titular ${id}`,
  tipo: 'AHORRO',
  saldo,
  estado: 'ACTIVA',
  ...extra,
});

async function render(repositorio: CuentaRepository): Promise<ComponentFixture<CuentasComponent>> {
  TestBed.configureTestingModule({
    imports: [CuentasComponent],
    providers: [{ provide: CuentaRepository, useValue: repositorio }],
  });
  const fixture = TestBed.createComponent(CuentasComponent);
  await fixture.whenStable();
  return fixture;
}

const texto = (fixture: ComponentFixture<CuentasComponent>): string =>
  (fixture.nativeElement as HTMLElement).textContent ?? '';

describe('CuentasComponent', () => {
  it('debe_mostrar_numero_titular_tipo_saldo_y_estado_de_cada_cuenta_pidiendo_el_tamano_maximo_de_pagina', async () => {
    const repositorio = new FakeCuentaRepository([
      cuenta(1, 1500.5),
      cuenta(2, 20, { tipo: 'CORRIENTE', estado: 'INACTIVA' }),
    ]);

    const fixture = await render(repositorio);

    const filas = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(filas).toHaveLength(2);
    expect(filas[0].textContent).toContain('1000000001');
    expect(filas[0].textContent).toContain('Titular 1');
    expect(filas[0].textContent).toContain('AHORRO');
    expect(filas[0].textContent).toContain('1,500.50');
    expect(filas[0].textContent).toContain('ACTIVA');
    expect(filas[1].textContent).toContain('CORRIENTE');
    expect(filas[1].textContent).toContain('INACTIVA');
    expect(repositorio.llamadas).toEqual([{ page: 0, size: TAMANO_PAGINA_MAXIMO }]);
  });

  it('debe_ordenar_por_saldo_ascendente_y_luego_descendente_cuando_se_pulsa_el_encabezado', async () => {
    const fixture = await render(
      new FakeCuentaRepository([cuenta(1, 500), cuenta(2, 100), cuenta(3, 900)]),
    );
    const boton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      'button[aria-label="Ordenar por saldo"]',
    );
    const ids = () =>
      Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr td:first-child'),
      ).map((td) => td.textContent?.trim());

    expect(ids()).toEqual(['1000000001', '1000000002', '1000000003']);

    boton?.click();
    await fixture.whenStable();
    expect(ids()).toEqual(['1000000002', '1000000001', '1000000003']);

    boton?.click();
    await fixture.whenStable();
    expect(ids()).toEqual(['1000000003', '1000000001', '1000000002']);
  });

  it('debe_mostrar_el_mensaje_de_error_y_ninguna_fila_cuando_falla_la_carga', async () => {
    const fixture = await render(new FakeCuentaRepository([], new ErrorDeRed()));

    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('No se pudo conectar con el servidor');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr')).toHaveLength(0);
  });

  it('debe_mostrar_mensaje_de_sin_cuentas_cuando_la_lista_esta_vacia', async () => {
    const fixture = await render(new FakeCuentaRepository([]));

    expect(texto(fixture)).toContain('No hay cuentas registradas');
  });

  it('debe_mostrar_indicador_de_carga_mientras_se_espera_la_respuesta', async () => {
    const fixture = await render(new FakeCuentaRepository([], 'pendiente'));

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[role="progressbar"]'),
    ).not.toBeNull();
    expect(texto(fixture)).not.toContain('No hay cuentas registradas');
  });

  it('debe_avisar_que_se_muestra_un_subconjunto_cuando_hay_mas_cuentas_que_las_cargadas', async () => {
    const repositorio = new FakeCuentaRepository([cuenta(1, 1), cuenta(2, 2), cuenta(3, 3)]);
    vi.spyOn(repositorio, 'listar').mockReturnValue(
      of({
        content: [cuenta(1, 1), cuenta(2, 2)],
        page: 0,
        size: 2,
        totalElements: 450,
        totalPages: 225,
      }),
    );

    const fixture = await render(repositorio);

    expect(texto(fixture)).toContain('Mostrando 2 de 450 cuentas');
  });
});
