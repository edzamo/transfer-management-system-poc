import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { CuentaService } from '../../application/cuenta.service';
import { Cuenta } from '../../domain/models/cuenta';
import { TAMANO_PAGINA_MAXIMO } from '../../domain/models/page';
import { OrdenSaldo, ordenarPorSaldo } from '../../domain/rules/ordenar-cuentas';

@Component({
  selector: 'app-cuentas',
  imports: [CurrencyPipe, MatTableModule, MatButtonModule, MatProgressBarModule],
  templateUrl: './cuentas.component.html',
  styleUrl: './cuentas.component.scss',
})
export class CuentasComponent implements OnInit {
  private readonly servicio = inject(CuentaService);

  protected readonly columnas = ['numero', 'titular', 'tipo', 'saldo', 'estado'];
  private readonly cuentas = signal<Cuenta[]>([]);
  protected readonly total = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly orden = signal<OrdenSaldo>(null);
  protected readonly filas = computed(() => ordenarPorSaldo(this.cuentas(), this.orden()));
  protected readonly hayMasDeLasCargadas = computed(() => this.total() > this.cuentas().length);

  ngOnInit(): void {
    this.servicio.listar(0, TAMANO_PAGINA_MAXIMO).subscribe({
      next: (pagina) => {
        this.cuentas.set(pagina.content);
        this.total.set(pagina.totalElements);
        this.cargando.set(false);
      },
      error: (error: Error) => {
        this.error.set(error.message);
        this.cargando.set(false);
      },
    });
  }

  protected alternarOrden(): void {
    this.orden.update((actual) => (actual === 'asc' ? 'desc' : 'asc'));
  }

  protected ariaSort(): 'ascending' | 'descending' | 'none' {
    const orden = this.orden();
    return orden === 'asc' ? 'ascending' : orden === 'desc' ? 'descending' : 'none';
  }
}
