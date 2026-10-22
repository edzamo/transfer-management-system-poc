package com.netby.transferencias.domain.transferencia;

import com.netby.transferencias.domain.dinero.Dinero;
import java.math.BigDecimal;

/** RN-3: sospechosa si el monto supera 5000 o el origen ya hizo 5 transferencias en la última hora. */
public class DeteccionSospechosaService {

    public static final Dinero MONTO_SOSPECHOSO = Dinero.de(new BigDecimal("5000"));
    public static final long MAX_PREVIAS_POR_HORA = 5;

    public boolean esSospechosa(Dinero monto, long previasUltimaHora) {
        return monto.esMayorQue(MONTO_SOSPECHOSO) || previasUltimaHora >= MAX_PREVIAS_POR_HORA;
    }
}
