package com.netby.transferencias.domain.shared;

import com.netby.transferencias.domain.error.DatosInvalidosException;

/** Solicitud de página validada: page >= 0 y 1 <= size <= 200. */
public record Paginacion(int page, int size) {

    public static final int TAMANO_MAXIMO = 200;

    public Paginacion {
        if (page < 0) {
            throw new DatosInvalidosException("page", "La página no puede ser negativa");
        }
        if (size < 1 || size > TAMANO_MAXIMO) {
            throw new DatosInvalidosException("size", "El tamaño debe estar entre 1 y " + TAMANO_MAXIMO);
        }
    }
}
