import { SolicitudInvalidaError } from './solicitud-invalida.error';

describe('SolicitudInvalidaError', () => {
  it('debe_conservar_los_errores_de_validacion_cuando_la_solicitud_es_invalida', () => {
    const error = new SolicitudInvalidaError([{ campo: 'monto', codigo: 'MONTO_INVALIDO' }]);

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe('SolicitudInvalidaError');
    expect(error.errores).toEqual([{ campo: 'monto', codigo: 'MONTO_INVALIDO' }]);
  });
});
