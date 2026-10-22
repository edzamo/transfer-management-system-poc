import { ErrorDeRed } from './error-de-red.error';
import { ErrorDeApi } from './error-de-api.error';

describe('errores de dominio', () => {
  it('debe_conservar_codigo_estado_detalles_y_monto_restante_cuando_se_rechaza_una_transferencia', () => {
    const error = new ErrorDeApi({
      status: 422,
      code: 'LIMITE_DIARIO_EXCEDIDO',
      message: 'Límite diario excedido',
      details: [{ field: 'monto', message: 'supera el límite' }],
      montoRestante: 250,
    });

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe('ErrorDeApi');
    expect(error.message).toBe('Límite diario excedido');
    expect(error.status).toBe(422);
    expect(error.code).toBe('LIMITE_DIARIO_EXCEDIDO');
    expect(error.details).toEqual([{ field: 'monto', message: 'supera el límite' }]);
    expect(error.montoRestante).toBe(250);
  });

  it('debe_usar_valores_por_defecto_cuando_el_rechazo_no_trae_detalles', () => {
    const error = new ErrorDeApi({
      status: 404,
      code: 'CUENTA_NO_ENCONTRADA',
      message: 'No existe',
    });

    expect(error.details).toEqual([]);
    expect(error.montoRestante).toBeNull();
  });

  it('debe_ser_un_error_distinguible_cuando_no_hay_conexion', () => {
    const error = new ErrorDeRed();

    expect(error).toBeInstanceOf(Error);
    expect(error).not.toBeInstanceOf(ErrorDeApi);
    expect(error.name).toBe('ErrorDeRed');
    expect(error.message).toBe('No se pudo conectar con el servidor');
  });
});
