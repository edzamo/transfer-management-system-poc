package com.netby.transferencias.domain.transferencia;

import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.dinero.Dinero;

/** RN-2: la transferencia procede solo si ambas cuentas están activas y el origen tiene saldo suficiente. */
public final class PoliticaTransferencia {

    private PoliticaTransferencia() {
    }

    public static void validar(Cuenta origen, Cuenta destino, Dinero monto) {
        origen.verificarActiva();
        destino.verificarActiva();
        origen.verificarSaldoPara(monto);
    }
}
