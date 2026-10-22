import { EstadoTransferencia } from './transferencia';

describe('EstadoTransferencia', () => {
  it('debe_admitir_solo_estados_de_exito_porque_el_backend_rechaza_con_422', () => {
    const exitosos: EstadoTransferencia[] = ['COMPLETADA', 'EN_REVISION'];
    // @ts-expect-error RECHAZADA no es un estado de un 201: se rechaza con 422 (ErrorDeApi).
    const invalido: EstadoTransferencia = 'RECHAZADA';

    expect(exitosos).toHaveLength(2);
    expect(invalido).toBeDefined();
  });
});
