import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { FakeTransferenciaRepository } from '../../testing/fake-transferencia.repository';
import { SolicitudInvalidaError } from '../domain/errors/solicitud-invalida.error';
import { ErrorDeApi } from '../domain/errors/error-de-api.error';
import { TransferenciaRepository } from '../domain/ports/transferencia.repository';
import { TransferenciaService } from './transferencia.service';

function crearServicio(repositorio: FakeTransferenciaRepository): TransferenciaService {
  TestBed.configureTestingModule({
    providers: [{ provide: TransferenciaRepository, useValue: repositorio }],
  });
  return TestBed.inject(TransferenciaService);
}

describe('TransferenciaService', () => {
  it('debe_enviar_la_solicitud_con_monto_numerico_y_devolver_la_transferencia_cuando_el_borrador_es_valido', async () => {
    const repositorio = new FakeTransferenciaRepository();
    const servicio = crearServicio(repositorio);

    const transferencia = await firstValueFrom(
      servicio.ejecutar({
        cuentaOrigen: '1000000001',
        cuentaDestino: '1000000002',
        monto: '150.5',
      }),
    );

    expect(repositorio.solicitudes).toEqual([
      { cuentaOrigen: '1000000001', cuentaDestino: '1000000002', monto: 150.5 },
    ]);
    expect(transferencia.estado).toBe('COMPLETADA');
    expect(transferencia.monto).toBe(150.5);
  });

  it('debe_fallar_con_solicitud_invalida_sin_llamar_al_puerto_cuando_el_borrador_no_cumple_la_regla', async () => {
    const repositorio = new FakeTransferenciaRepository();
    const servicio = crearServicio(repositorio);

    const resultado = firstValueFrom(
      servicio.ejecutar({ cuentaOrigen: '1000000001', cuentaDestino: '1000000001', monto: 0 }),
    );

    await expect(resultado).rejects.toBeInstanceOf(SolicitudInvalidaError);
    await expect(resultado).rejects.toMatchObject({
      errores: [
        { campo: 'monto', codigo: 'MONTO_INVALIDO' },
        { campo: 'cuentaDestino', codigo: 'MISMA_CUENTA' },
      ],
    });
    expect(repositorio.solicitudes).toEqual([]);
  });

  // Caracterizacion (no es RED): el error del backend se propaga tal cual al llamador.
  it('debe_propagar_el_rechazo_del_backend_cuando_el_puerto_falla', async () => {
    const rechazo = new ErrorDeApi({
      status: 422,
      code: 'SALDO_INSUFICIENTE',
      message: 'Saldo insuficiente',
    });
    const servicio = crearServicio(new FakeTransferenciaRepository({ fallo: rechazo }));

    await expect(
      firstValueFrom(servicio.ejecutar({ cuentaOrigen: 'A', cuentaDestino: 'B', monto: 5 })),
    ).rejects.toBe(rechazo);
  });
});
