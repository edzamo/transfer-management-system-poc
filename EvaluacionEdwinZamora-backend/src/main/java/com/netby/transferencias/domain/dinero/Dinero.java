package com.netby.transferencias.domain.dinero;

import com.netby.transferencias.domain.error.DatosInvalidosException;
import java.math.BigDecimal;

/** Monto monetario: escala exacta 2 y nunca negativo. Más de 2 decimales se rechaza (no se redondea). */
public record Dinero(BigDecimal valor) implements Comparable<Dinero> {

    private static final int ESCALA = 2;
    /** DECIMAL(19,2): como máximo 17 dígitos enteros. */
    private static final int DIGITOS_ENTEROS_MAXIMOS = 17;
    private static final String CAMPO_POR_DEFECTO = "monto";

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public Dinero {
        valor = normalizar(valor, CAMPO_POR_DEFECTO);
    }

    public static Dinero de(BigDecimal valor) {
        return new Dinero(valor);
    }

    /** Igual que {@link #de(BigDecimal)} pero atribuye el error al campo indicado (p. ej. saldoInicial). */
    public static Dinero de(BigDecimal valor, String campo) {
        return new Dinero(normalizar(valor, campo));
    }

    private static BigDecimal normalizar(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new DatosInvalidosException(campo, "El campo " + campo + " es obligatorio");
        }
        // Sin operar sobre la magnitud: precision y scale son O(1) y evitan setScale/strip con exponentes enormes.
        if (valor.signum() != 0 && (long) valor.precision() - valor.scale() > DIGITOS_ENTEROS_MAXIMOS) {
            throw new DatosInvalidosException(campo, "El campo " + campo + " excede el valor máximo permitido");
        }
        if (valor.stripTrailingZeros().scale() > ESCALA) {
            throw new DatosInvalidosException(campo, "El campo " + campo + " admite como máximo 2 decimales");
        }
        BigDecimal normalizado = valor.setScale(ESCALA);
        if (normalizado.signum() < 0) {
            throw new DatosInvalidosException(campo, "El campo " + campo + " no puede ser negativo");
        }
        return normalizado;
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(valor.add(otro.valor));
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(valor.subtract(otro.valor));
    }

    public boolean esMayorQue(Dinero otro) {
        return valor.compareTo(otro.valor) > 0;
    }

    public boolean esPositivo() {
        return valor.signum() > 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        return valor.compareTo(otro.valor);
    }
}
