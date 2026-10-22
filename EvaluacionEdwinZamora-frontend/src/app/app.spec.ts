import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App (shell)', () => {
  it('debe_mostrar_la_barra_de_navegacion_con_enlaces_a_cuentas_y_transferencias', async () => {
    TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const enlaces = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a')).map(
      (a) => [a.textContent?.trim(), a.getAttribute('href')],
    );
    expect(enlaces).toEqual([
      ['Cuentas', '/cuentas'],
      ['Transferencias', '/transferencias'],
    ]);
  });
});
