import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FakeCuentaRepository } from '../../../testing/fake-cuenta.repository';
import {
  FakeTransferenciaRepository,
  OpcionesFakeTransferencia,
} from '../../../testing/fake-transferencia.repository';
import { ErrorDeRed } from '../../domain/errors/error-de-red.error';
import { ErrorDeApi } from '../../domain/errors/error-de-api.error';
import { Cuenta } from '../../domain/models/cuenta';
import { CuentaRepository } from '../../domain/ports/cuenta.repository';
import { TransferenciaRepository } from '../../domain/ports/transferencia.repository';
import { TransferenciaFormComponent } from './transferencia-form.component';

const cuenta = (id: number): Cuenta => ({
  id,
  numero: `100000000${id}`,
  titular: `Titular ${id}`,
  tipo: 'AHORRO',
  saldo: 1000,
  estado: 'ACTIVA',
});

interface Escenario {
  fixture: ComponentFixture<TransferenciaFormComponent>;
  el: HTMLElement;
  transferencias: FakeTransferenciaRepository;
}

async function render(
  opciones: OpcionesFakeTransferencia = {},
  cuentas: FakeCuentaRepository = new FakeCuentaRepository([cuenta(1), cuenta(2), cuenta(3)]),
): Promise<Escenario> {
  const transferencias = new FakeTransferenciaRepository(opciones);
  TestBed.configureTestingModule({
    imports: [TransferenciaFormComponent],
    providers: [
      { provide: CuentaRepository, useValue: cuentas },
      { provide: TransferenciaRepository, useValue: transferencias },
    ],
  });
  const fixture = TestBed.createComponent(TransferenciaFormComponent);
  await fixture.whenStable();
  return { fixture, el: fixture.nativeElement as HTMLElement, transferencias };
}

async function llenar(
  { fixture, el }: Escenario,
  valores: { origen?: string; destino?: string; monto?: string },
): Promise<void> {
  const asignar = (selector: string, valor: string | undefined, evento: string) => {
    const control = el.querySelector<HTMLInputElement | HTMLSelectElement>(selector);
    if (control && valor !== undefined) {
      control.value = valor;
      control.dispatchEvent(new Event(evento));
    }
  };
  asignar('select[formControlName="cuentaOrigen"]', valores.origen, 'change');
  asignar('select[formControlName="cuentaDestino"]', valores.destino, 'change');
  asignar('input[formControlName="monto"]', valores.monto, 'input');
  await fixture.whenStable();
}

const resultado = (el: HTMLElement): HTMLElement | null =>
  el.querySelector('[data-testid="resultado"]');

async function enviar({ fixture, el }: Escenario): Promise<void> {
  el.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
  await fixture.whenStable();
}

describe('TransferenciaFormComponent', () => {
  it('debe_ofrecer_las_cuentas_como_opciones_de_origen_y_destino', async () => {
    const { el } = await render();

    const origen = el.querySelector<HTMLSelectElement>('select[formControlName="cuentaOrigen"]');
    const destino = el.querySelector<HTMLSelectElement>('select[formControlName="cuentaDestino"]');
    const valores = (select: HTMLSelectElement | null) =>
      Array.from(select?.querySelectorAll('option') ?? [])
        .map((o) => o.value)
        .filter((v) => v !== '');
    expect(valores(origen)).toEqual(['1000000001', '1000000002', '1000000003']);
    expect(valores(destino)).toEqual(['1000000001', '1000000002', '1000000003']);
    expect(origen?.textContent).toContain('1000000001 - Titular 1');
    expect(el.querySelector('input[formControlName="monto"]')).not.toBeNull();
    expect(el.querySelector('button[type="submit"]')?.textContent).toContain('Transferir');
  });

  it('debe_mostrar_requerido_en_cada_campo_y_no_llamar_al_backend_cuando_se_envia_vacio', async () => {
    const escenario = await render();

    await enviar(escenario);

    const texto = escenario.el.textContent ?? '';
    expect(texto).toContain('Seleccione la cuenta origen');
    expect(texto).toContain('Seleccione la cuenta destino');
    expect(texto).toContain('Ingrese el monto');
    expect(escenario.transferencias.solicitudes).toEqual([]);
  });

  it.each(['0', '-3', 'abc'])(
    'debe_mostrar_monto_invalido_y_no_llamar_al_backend_cuando_el_monto_es_%s',
    async (monto) => {
      const escenario = await render();
      await llenar(escenario, { origen: '1000000001', destino: '1000000002', monto });

      await enviar(escenario);

      expect(escenario.el.textContent).toContain('El monto debe ser un número mayor a cero');
      expect(escenario.transferencias.solicitudes).toEqual([]);
    },
  );

  it('debe_mostrar_error_y_no_llamar_al_backend_cuando_origen_y_destino_son_la_misma_cuenta', async () => {
    const escenario = await render();
    await llenar(escenario, { origen: '1000000001', destino: '1000000001', monto: '10' });

    await enviar(escenario);

    expect(escenario.el.textContent).toContain(
      'La cuenta destino debe ser distinta de la cuenta origen',
    );
    expect(escenario.transferencias.solicitudes).toEqual([]);
  });

  it('debe_enviar_la_solicitud_y_mostrar_exito_cuando_la_transferencia_se_completa', async () => {
    const escenario = await render();
    await llenar(escenario, { origen: '1000000001', destino: '1000000002', monto: '150.5' });

    await enviar(escenario);

    expect(escenario.transferencias.solicitudes).toEqual([
      { cuentaOrigen: '1000000001', cuentaDestino: '1000000002', monto: 150.5 },
    ]);
    const mensaje = resultado(escenario.el);
    expect(mensaje?.getAttribute('data-tipo')).toBe('exito');
    expect(mensaje?.textContent).toContain('Transferencia completada');
  });

  it('debe_mostrar_advertencia_con_el_motivo_cuando_el_201_trae_EN_REVISION', async () => {
    const escenario = await render({ estado: 'EN_REVISION', motivo: 'Monto superior a 5000' });
    await llenar(escenario, { origen: '1000000001', destino: '1000000002', monto: '6000' });

    await enviar(escenario);

    const mensaje = resultado(escenario.el);
    expect(mensaje?.getAttribute('data-tipo')).toBe('advertencia');
    expect(mensaje?.textContent).toContain('Transferencia en revisión');
    expect(mensaje?.textContent).toContain('Monto superior a 5000');
  });

  it('debe_mostrar_el_mensaje_del_backend_como_error_cuando_la_transferencia_es_rechazada_con_422', async () => {
    const fallo = new ErrorDeApi({
      status: 422,
      code: 'SALDO_INSUFICIENTE',
      message: 'Saldo insuficiente en la cuenta origen',
    });
    const escenario = await render({ fallo });
    await llenar(escenario, { origen: '1000000001', destino: '1000000002', monto: '5000' });

    await enviar(escenario);

    const mensaje = resultado(escenario.el);
    expect(mensaje?.getAttribute('data-tipo')).toBe('error');
    expect(mensaje?.textContent).toContain('Saldo insuficiente en la cuenta origen');
  });

  it('debe_mostrar_el_mensaje_del_backend_tal_cual_sin_duplicar_el_monto_restante_cuando_se_excede_el_limite_diario', async () => {
    const fallo = new ErrorDeApi({
      status: 422,
      code: 'LIMITE_DIARIO_EXCEDIDO',
      message: 'Límite diario excedido. Monto restante disponible: 1500.50',
      montoRestante: 1500.5,
    });
    const escenario = await render({ fallo });
    await llenar(escenario, { origen: '1000000001', destino: '1000000002', monto: '9000' });

    await enviar(escenario);

    const texto = resultado(escenario.el)?.textContent ?? '';
    expect(texto).toContain('Límite diario excedido. Monto restante disponible: 1500.50');
    expect(texto.match(/Monto restante/g)).toHaveLength(1);
  });

  // Caracterizacion (no es RED): ya cubierto por el manejo generico de errores.
  it('debe_mostrar_el_error_de_red_cuando_no_hay_conexion', async () => {
    const escenario = await render({ fallo: new ErrorDeRed() });
    await llenar(escenario, { origen: '1000000001', destino: '1000000002', monto: '10' });

    await enviar(escenario);

    expect(resultado(escenario.el)?.getAttribute('data-tipo')).toBe('error');
    expect(resultado(escenario.el)?.textContent).toContain('No se pudo conectar con el servidor');
  });

  it('debe_deshabilitar_el_boton_y_evitar_envios_duplicados_mientras_espera_la_respuesta', async () => {
    const escenario = await render({ pendiente: true });
    await llenar(escenario, { origen: '1000000001', destino: '1000000002', monto: '10' });

    await enviar(escenario);
    await enviar(escenario);

    expect(escenario.el.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(
      true,
    );
    expect(escenario.transferencias.solicitudes).toHaveLength(1);
  });

  it('debe_limpiar_el_formulario_cuando_la_transferencia_se_acepta_y_conservarlo_cuando_falla', async () => {
    const aceptada = await render();
    await llenar(aceptada, { origen: '1000000001', destino: '1000000002', monto: '10' });
    await enviar(aceptada);
    expect(
      aceptada.el.querySelector<HTMLInputElement>('input[formControlName="monto"]')?.value,
    ).toBe('');
    expect(
      aceptada.el.querySelector<HTMLSelectElement>('select[formControlName="cuentaOrigen"]')?.value,
    ).toBe('');
  });

  it('debe_conservar_los_valores_cuando_el_backend_rechaza', async () => {
    const fallo = new ErrorDeApi({
      status: 422,
      code: 'SALDO_INSUFICIENTE',
      message: 'Saldo insuficiente',
    });
    const rechazada = await render({ fallo });
    await llenar(rechazada, { origen: '1000000001', destino: '1000000002', monto: '77' });
    await enviar(rechazada);
    expect(
      rechazada.el.querySelector<HTMLInputElement>('input[formControlName="monto"]')?.value,
    ).toBe('77');
  });

  it('debe_mostrar_el_error_de_carga_cuando_no_se_pueden_obtener_las_cuentas', async () => {
    const { el } = await render({}, new FakeCuentaRepository([], new ErrorDeRed()));

    expect(el.querySelector('[data-testid="error-cuentas"]')?.textContent).toContain(
      'No se pudo conectar con el servidor',
    );
  });
});
