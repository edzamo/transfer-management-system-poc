package com.netby.transferencias.domain.error;

import java.math.BigDecimal;

public final class LimiteDiarioExcedidoException extends ReglaNegocioException {

    public static final String CODE = "LIMITE_DIARIO_EXCEDIDO";

    private final BigDecimal montoRestante;

    public LimiteDiarioExcedidoException(BigDecimal montoRestante) {
        super(CODE, "Límite diario excedido. Monto restante disponible: " + montoRestante.toPlainString());
        this.montoRestante = montoRestante;
    }

    public BigDecimal montoRestante() {
        return montoRestante;
    }
}
