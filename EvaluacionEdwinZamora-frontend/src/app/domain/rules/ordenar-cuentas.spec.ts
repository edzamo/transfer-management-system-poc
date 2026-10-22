import { Cuenta } from '../models/cuenta';
import { ordenarPorSaldo } from './ordenar-cuentas';

const cuenta = (id: number, saldo: number): Cuenta => ({
  id,
  numero: `100000000${id}`,
  titular: `T${id}`,
  tipo: 'CORRIENTE',
  saldo,
  estado: 'ACTIVA',
});

describe('ordenarPorSaldo', () => {
  const cuentas = [cuenta(1, 500), cuenta(2, 100.5), cuenta(3, 900)];

  it('debe_ordenar_de_menor_a_mayor_cuando_el_orden_es_asc', () => {
    expect(ordenarPorSaldo(cuentas, 'asc').map((c) => c.id)).toEqual([2, 1, 3]);
  });

  it('debe_ordenar_de_mayor_a_menor_cuando_el_orden_es_desc', () => {
    expect(ordenarPorSaldo(cuentas, 'desc').map((c) => c.id)).toEqual([3, 1, 2]);
  });

  it('debe_conservar_el_orden_original_sin_mutar_la_entrada_cuando_no_hay_orden', () => {
    const copia = [...cuentas];

    expect(ordenarPorSaldo(cuentas, null)).toEqual(copia);
    ordenarPorSaldo(cuentas, 'desc');
    expect(cuentas).toEqual(copia);
  });
});
