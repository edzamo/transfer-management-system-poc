package com.netby.transferencias.domain.cuenta;

import com.netby.transferencias.domain.error.DatosInvalidosException;
import java.util.regex.Pattern;

/** Número de cuenta: exactamente 10 dígitos. */
public record NumeroCuenta(String valor) implements Comparable<NumeroCuenta> {

    private static final Pattern DIEZ_DIGITOS = Pattern.compile("\\d{10}");
    private static final int DIGITOS_VISIBLES = 4;

    public NumeroCuenta {
        if (valor == null || !DIEZ_DIGITOS.matcher(valor).matches()) {
            throw new DatosInvalidosException("numero", "El número de cuenta debe tener exactamente 10 dígitos");
        }
    }

    public static NumeroCuenta de(String valor) {
        return new NumeroCuenta(valor);
    }

    /** Para logs: oculta todo salvo los últimos 4 dígitos. */
    public String enmascarado() {
        return "*".repeat(valor.length() - DIGITOS_VISIBLES) + valor.substring(valor.length() - DIGITOS_VISIBLES);
    }

    @Override
    public int compareTo(NumeroCuenta otro) {
        return valor.compareTo(otro.valor);
    }
}
