package com.netby.transferencias.domain.error;

/** La cuenta está siendo modificada por otra operación y no se pudo obtener su bloqueo a tiempo. */
public final class CuentaBloqueadaException extends DominioException {

    public static final String CODE = "CUENTA_BLOQUEADA";

    public CuentaBloqueadaException(String message) {
        super(CODE, message);
    }
}
