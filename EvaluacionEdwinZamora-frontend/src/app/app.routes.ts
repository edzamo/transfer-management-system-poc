import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'cuentas', pathMatch: 'full' },
  {
    path: 'cuentas',
    loadComponent: () =>
      import('./presentation/cuentas/cuentas.component').then((m) => m.CuentasComponent),
  },
  {
    path: 'transferencias',
    loadComponent: () =>
      import('./presentation/transferencia-form/transferencia-form.component').then(
        (m) => m.TransferenciaFormComponent,
      ),
  },
];
