package com.netby.transferencias.domain.transferencia;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import com.netby.transferencias.domain.cuenta.Titular;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaInactivaException;
import com.netby.transferencias.domain.error.SaldoInsuficienteException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PoliticaTransferenciaTest {

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    private static Cuenta cuenta(String numero, String saldo, EstadoCuenta estado) {
        return Cuenta.abrir(NumeroCuenta.de(numero), Titular.de("T"), TipoCuenta.AHORRO, d(saldo), estado);
    }

    @Test
    void debe_proceder_con_ambas_activas_y_saldo_justo() {
        assertThatCode(() -> PoliticaTransferencia.validar(
                cuenta("0000000001", "100", EstadoCuenta.ACTIVA),
                cuenta("0000000002", "0", EstadoCuenta.ACTIVA), d("100"))).doesNotThrowAnyException();
    }

    @Test
    void debe_fallar_si_el_origen_esta_inactivo() {
        assertThatThrownBy(() -> PoliticaTransferencia.validar(
                cuenta("0000000001", "100", EstadoCuenta.INACTIVA),
                cuenta("0000000002", "0", EstadoCuenta.ACTIVA), d("1")))
                .isInstanceOf(CuentaInactivaException.class);
    }

    @Test
    void debe_fallar_si_el_destino_esta_inactivo() {
        assertThatThrownBy(() -> PoliticaTransferencia.validar(
                cuenta("0000000001", "100", EstadoCuenta.ACTIVA),
                cuenta("0000000002", "0", EstadoCuenta.INACTIVA), d("1")))
                .isInstanceOf(CuentaInactivaException.class);
    }

    @Test
    void debe_fallar_si_el_origen_no_tiene_saldo_suficiente() {
        assertThatThrownBy(() -> PoliticaTransferencia.validar(
                cuenta("0000000001", "99.99", EstadoCuenta.ACTIVA),
                cuenta("0000000002", "0", EstadoCuenta.ACTIVA), d("100")))
                .isInstanceOf(SaldoInsuficienteException.class);
    }
}
