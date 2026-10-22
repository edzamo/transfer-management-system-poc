package com.netby.transferencias.domain.error;

public final class CuentaInactivaException extends ReglaNegocioException {

    public static final String CODE = "CUENTA_INACTIVA";

    public CuentaInactivaException(String message) {
        super(CODE, message);
    }
}
