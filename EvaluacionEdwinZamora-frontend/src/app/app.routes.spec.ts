import { routes } from './app.routes';
import { CuentasComponent } from './presentation/cuentas/cuentas.component';
import { TransferenciaFormComponent } from './presentation/transferencia-form/transferencia-form.component';

describe('routes', () => {
  it('debe_exponer_cuentas_y_transferencias_y_redirigir_la_raiz_a_cuentas', async () => {
    const rutas = Object.fromEntries(routes.map((r) => [r.path, r]));

    expect(rutas['']).toMatchObject({ redirectTo: 'cuentas', pathMatch: 'full' });
    const cuentas = await rutas['cuentas'].loadComponent?.();
    const transferencias = await rutas['transferencias'].loadComponent?.();
    expect(cuentas).toBe(CuentasComponent);
    expect(transferencias).toBe(TransferenciaFormComponent);
  });
});
