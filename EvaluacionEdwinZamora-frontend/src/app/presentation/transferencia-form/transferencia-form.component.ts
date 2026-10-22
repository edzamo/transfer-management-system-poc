import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CuentaService } from '../../application/cuenta.service';
import { TransferenciaService } from '../../application/transferencia.service';
import { Cuenta } from '../../domain/models/cuenta';
import { TAMANO_PAGINA_MAXIMO } from '../../domain/models/page';
import { EstadoTransferencia, Transferencia } from '../../domain/models/transferencia';
import {
  BorradorTransferencia,
  ErrorValidacion,
  validarTransferencia,
} from '../../domain/rules/validar-transferencia';

interface Resultado {
  tipo: 'exito' | 'advertencia' | 'error';
  mensaje: string;
}

const TITULOS: Record<EstadoTransferencia, string> = {
  COMPLETADA: 'Transferencia completada',
  EN_REVISION: 'Transferencia en revisión',
};

/** El 201 es COMPLETADA (exito) o EN_REVISION (advertencia con motivo si existe); el rechazo llega como 422. */
function resultadoDe(transferencia: Transferencia): Resultado {
  const titulo = TITULOS[transferencia.estado];
  if (transferencia.estado === 'COMPLETADA') {
    return { tipo: 'exito', mensaje: titulo };
  }
  return {
    tipo: 'advertencia',
    mensaje: `${titulo}: ${transferencia.motivo ?? 'sin motivo informado'}`,
  };
}

const MENSAJES: Record<string, string> = {
  'cuentaOrigen:REQUERIDO': 'Seleccione la cuenta origen',
  'cuentaDestino:REQUERIDO': 'Seleccione la cuenta destino',
  'monto:REQUERIDO': 'Ingrese el monto',
  'cuentaDestino:MISMA_CUENTA': 'La cuenta destino debe ser distinta de la cuenta origen',
  'monto:MONTO_INVALIDO': 'El monto debe ser un número mayor a cero',
};

@Component({
  selector: 'app-transferencia-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './transferencia-form.component.html',
  styleUrl: './transferencia-form.component.scss',
})
export class TransferenciaFormComponent implements OnInit {
  private readonly cuentaService = inject(CuentaService);
  private readonly transferenciaService = inject(TransferenciaService);

  protected readonly cuentas = signal<Cuenta[]>([]);
  protected readonly errorCuentas = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly resultado = signal<Resultado | null>(null);
  protected readonly erroresValidacion = signal<ErrorValidacion[]>([]);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    cuentaOrigen: '',
    cuentaDestino: '',
    monto: '',
  });

  ngOnInit(): void {
    this.cuentaService.listar(0, TAMANO_PAGINA_MAXIMO).subscribe({
      next: (pagina) => this.cuentas.set(pagina.content),
      error: (error: Error) => this.errorCuentas.set(error.message),
    });
  }

  protected enviar(): void {
    if (this.enviando()) {
      return;
    }
    const borrador: BorradorTransferencia = this.form.getRawValue();
    const errores = validarTransferencia(borrador);
    this.erroresValidacion.set(errores);
    if (errores.length > 0) {
      return;
    }
    this.enviando.set(true);
    this.transferenciaService.ejecutar(borrador).subscribe({
      next: (transferencia) => {
        this.resultado.set(resultadoDe(transferencia));
        this.form.reset();
        this.enviando.set(false);
      },
      error: (error: Error) => {
        this.resultado.set({ tipo: 'error', mensaje: error.message });
        this.enviando.set(false);
      },
    });
  }

  protected mensajeDe(campo: keyof BorradorTransferencia): string | undefined {
    const error = this.erroresValidacion().find((e) => e.campo === campo);
    return error && MENSAJES[`${error.campo}:${error.codigo}`];
  }
}
