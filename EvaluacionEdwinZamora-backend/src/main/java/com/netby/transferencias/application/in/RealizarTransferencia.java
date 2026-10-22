package com.netby.transferencias.application.in;

import com.netby.transferencias.domain.transferencia.Transferencia;
import java.math.BigDecimal;

public interface RealizarTransferencia {

    /** Devuelve la transferencia COMPLETADA o EN_REVISION; los rechazos por regla se lanzan como excepción. */
    Transferencia realizar(Command comando);

    record Command(String cuentaOrigen, String cuentaDestino, BigDecimal monto) {
    }
}
