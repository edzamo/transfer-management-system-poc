package com.netby.transferencias.domain.error;

public final class SaldoInsuficienteException extends ReglaNegocioException {

    public static final String CODE = "SALDO_INSUFICIENTE";

    public SaldoInsuficienteException(String message) {
        super(CODE, message);
    }
}
