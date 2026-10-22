package com.netby.transferencias.domain.transferencia;

import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.LimiteDiarioExcedidoException;
import java.math.BigDecimal;

/** RN-4: una cuenta no puede transferir más de 10000 acumulados por día calendario. */
public class LimiteDiarioService {

    public static final Dinero LIMITE_DIARIO = Dinero.de(new BigDecimal("10000"));

    public void verificar(Dinero acumuladoHoy, Dinero monto) {
        if (acumuladoHoy.sumar(monto).esMayorQue(LIMITE_DIARIO)) {
            throw new LimiteDiarioExcedidoException(restante(acumuladoHoy).valor());
        }
    }

    private Dinero restante(Dinero acumuladoHoy) {
        return acumuladoHoy.esMayorQue(LIMITE_DIARIO) ? Dinero.CERO : LIMITE_DIARIO.restar(acumuladoHoy);
    }
}
