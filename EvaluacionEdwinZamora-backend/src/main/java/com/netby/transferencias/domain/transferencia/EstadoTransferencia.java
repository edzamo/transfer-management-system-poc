package com.netby.transferencias.domain.transferencia;

import java.util.Set;

public enum EstadoTransferencia {
    COMPLETADA, RECHAZADA, EN_REVISION;

    /** Estados que mueven saldo y por tanto cuentan para el límite diario y la ventana de última hora (D1). */
    public static final Set<EstadoTransferencia> CONSUMEN_CUPO = Set.of(COMPLETADA, EN_REVISION);
}
