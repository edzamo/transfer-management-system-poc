package com.netby.transferencias.domain.error;

public final class CuentaNoEncontradaException extends DominioException {

    public static final String CODE = "CUENTA_NO_ENCONTRADA";

    public CuentaNoEncontradaException(String message) {
        super(CODE, message);
    }
}
