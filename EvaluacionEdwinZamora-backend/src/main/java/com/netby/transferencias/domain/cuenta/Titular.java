package com.netby.transferencias.domain.cuenta;

import com.netby.transferencias.domain.error.DatosInvalidosException;

/** Nombre del titular: no vacío, máximo 150 caracteres. Dato personal: no aparece en toString. */
public record Titular(String valor) {

    public static final int LONGITUD_MAXIMA = 150;

    public Titular {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("titular", "El titular es obligatorio");
        }
        valor = valor.strip();
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new DatosInvalidosException("titular", "El titular admite como máximo " + LONGITUD_MAXIMA + " caracteres");
        }
    }

    public static Titular de(String valor) {
        return new Titular(valor);
    }

    @Override
    public String toString() {
        return "Titular[***]";
    }
}
