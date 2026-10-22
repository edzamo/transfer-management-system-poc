import { validarTransferencia } from './validar-transferencia';

describe('validarTransferencia', () => {
  it('debe_no_reportar_errores_cuando_el_borrador_es_valido', () => {
    const errores = validarTransferencia({
      cuentaOrigen: '1000000001',
      cuentaDestino: '1000000002',
      monto: 100,
    });

    expect(errores).toEqual([]);
  });

  it('debe_reportar_requerido_por_cada_campo_vacio', () => {
    const errores = validarTransferencia({ cuentaOrigen: null, cuentaDestino: '', monto: null });

    expect(errores).toEqual([
      { campo: 'cuentaOrigen', codigo: 'REQUERIDO' },
      { campo: 'cuentaDestino', codigo: 'REQUERIDO' },
      { campo: 'monto', codigo: 'REQUERIDO' },
    ]);
  });

  it.each([0, -5, '0'])('debe_reportar_monto_invalido_cuando_el_monto_es_%s', (monto) => {
    const errores = validarTransferencia({ cuentaOrigen: 'A', cuentaDestino: 'B', monto });

    expect(errores).toEqual([{ campo: 'monto', codigo: 'MONTO_INVALIDO' }]);
  });

  it.each(['abc', '12,5', NaN, Infinity])(
    'debe_reportar_monto_invalido_cuando_no_es_numerico_%s',
    (monto) => {
      const errores = validarTransferencia({ cuentaOrigen: 'A', cuentaDestino: 'B', monto });

      expect(errores).toEqual([{ campo: 'monto', codigo: 'MONTO_INVALIDO' }]);
    },
  );

  it('debe_reportar_misma_cuenta_cuando_origen_y_destino_son_iguales', () => {
    const errores = validarTransferencia({
      cuentaOrigen: '1000000001',
      cuentaDestino: '1000000001',
      monto: 10,
    });

    expect(errores).toEqual([{ campo: 'cuentaDestino', codigo: 'MISMA_CUENTA' }]);
  });
});
