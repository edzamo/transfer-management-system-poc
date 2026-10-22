package com.netby.transferencias.domain.error;

/** Regla de negocio incumplida (HTTP 422). La transferencia afectada se registra como RECHAZADA. */
public abstract sealed class ReglaNegocioException extends DominioException
        permits CuentaInactivaException, SaldoInsuficienteException, LimiteDiarioExcedidoException {

    protected ReglaNegocioException(String code, String message) {
        super(code, message);
    }
}
