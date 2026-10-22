package com.netby.transferencias.domain.error;

/** Dato que viola una invariante de un Value Object o del agregado (HTTP 400). */
public final class DatosInvalidosException extends DominioException {

    public static final String CODE = "VALIDACION";

    private final String campo;

    public DatosInvalidosException(String campo, String message) {
        super(CODE, message);
        this.campo = campo;
    }

    /** Campo afectado; puede ser nulo si la violación no pertenece a un único campo. */
    public String campo() {
        return campo;
    }
}
