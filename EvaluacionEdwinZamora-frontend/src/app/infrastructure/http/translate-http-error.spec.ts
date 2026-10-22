import { HttpErrorResponse } from '@angular/common/http';
import { ErrorDeRed } from '../../domain/errors/error-de-red.error';
import { ErrorDeApi } from '../../domain/errors/error-de-api.error';
import { translateHttpError } from './translate-http-error';

const respuesta = (status: number, error: unknown) =>
  new HttpErrorResponse({ status, error, url: '/api/transferencias' });

describe('translateHttpError', () => {
  it('debe_traducir_a_ErrorDeRed_cuando_no_hay_respuesta_del_servidor', () => {
    const error = translateHttpError(
      new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }),
    );

    expect(error).toBeInstanceOf(ErrorDeRed);
  });

  it('debe_traducir_422_a_ErrorDeApi_con_codigo_mensaje_y_detalles', () => {
    const error = translateHttpError(
      respuesta(422, {
        timestamp: '2026-10-22T10:00:00Z',
        status: 422,
        code: 'SALDO_INSUFICIENTE',
        message: 'Saldo insuficiente en la cuenta origen',
        path: '/api/transferencias',
        details: [{ field: 'monto', message: 'excede el saldo' }],
      }),
    );

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error).toMatchObject({
      status: 422,
      code: 'SALDO_INSUFICIENTE',
      message: 'Saldo insuficiente en la cuenta origen',
      details: [{ field: 'monto', message: 'excede el saldo' }],
      montoRestante: null,
    });
  });

  it('debe_conservar_monto_restante_cuando_se_excede_el_limite_diario', () => {
    const error = translateHttpError(
      respuesta(422, {
        status: 422,
        code: 'LIMITE_DIARIO_EXCEDIDO',
        message: 'Límite excedido',
        montoRestante: 1500.5,
      }),
    );

    expect(error).toMatchObject({ code: 'LIMITE_DIARIO_EXCEDIDO', montoRestante: 1500.5 });
  });

  it.each([400, 404])('debe_traducir_%i_a_ErrorDeApi', (status) => {
    const error = translateHttpError(
      respuesta(status, { status, code: 'ERROR', message: 'detalle del backend' }),
    );

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error).toMatchObject({ status, message: 'detalle del backend' });
  });

  it('debe_usar_un_mensaje_generico_cuando_el_cuerpo_no_es_el_contrato_de_error', () => {
    const error = translateHttpError(respuesta(502, '<html>Bad Gateway</html>'));

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error).toMatchObject({
      status: 502,
      code: 'ERROR_DESCONOCIDO',
      message: 'Error inesperado del servidor (502)',
    });
  });
});
