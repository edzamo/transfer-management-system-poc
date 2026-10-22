package com.netby.transferencias.domain.error;

public final class CuentaDuplicadaException extends DominioException {

    public static final String CODE = "CUENTA_DUPLICADA";

    public CuentaDuplicadaException(String message) {
        super(CODE, message);
    }
}
