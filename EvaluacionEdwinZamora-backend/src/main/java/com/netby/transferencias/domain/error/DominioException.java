package com.netby.transferencias.domain.error;

/** Raíz de los errores de negocio. Cada variante porta un código estable para el cliente. */
public abstract sealed class DominioException extends RuntimeException
        permits DatosInvalidosException, CuentaNoEncontradaException, CuentaDuplicadaException,
                CuentaBloqueadaException, ReglaNegocioException {

    private final String code;

    protected DominioException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
