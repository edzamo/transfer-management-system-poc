package com.netby.transferencias.domain.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.CuentaInactivaException;
import com.netby.transferencias.domain.error.SaldoInsuficienteException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CuentaTest {

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    private static Cuenta cuenta(String saldo, EstadoCuenta estado) {
        return Cuenta.abrir(NumeroCuenta.de("0123456789"), Titular.de("Ana"), TipoCuenta.AHORRO, d(saldo), estado);
    }

    @Test
    void debe_abrir_cuenta_sin_id_con_los_datos_dados() {
        Cuenta c = cuenta("100", EstadoCuenta.ACTIVA);
        assertThat(c.id()).isNull();
        assertThat(c.numero().valor()).isEqualTo("0123456789");
        assertThat(c.tipo()).isEqualTo(TipoCuenta.AHORRO);
        assertThat(c.saldo()).isEqualTo(d("100"));
        assertThat(c.estaActiva()).isTrue();
    }

    @Test
    void debe_reconstruir_conservando_el_id() {
        Cuenta c = Cuenta.reconstruir(7L, NumeroCuenta.de("0123456789"), Titular.de("Ana"),
                TipoCuenta.CORRIENTE, d("0"), EstadoCuenta.INACTIVA);
        assertThat(c.id()).isEqualTo(7L);
        assertThat(c.estaActiva()).isFalse();
    }

    @Test
    void debe_reportar_si_el_saldo_alcanza() {
        Cuenta c = cuenta("100", EstadoCuenta.ACTIVA);
        assertThat(c.tieneSaldoPara(d("100"))).isTrue();
        assertThat(c.tieneSaldoPara(d("100.01"))).isFalse();
    }

    @Test
    void debe_debitar_y_acreditar() {
        Cuenta c = cuenta("100", EstadoCuenta.ACTIVA);
        c.debitar(d("40.50"));
        c.acreditar(d("0.25"));
        assertThat(c.saldo()).isEqualTo(d("59.75"));
    }

    @Test
    void debe_impedir_saldo_negativo_al_debitar_mas_de_lo_disponible() {
        Cuenta c = cuenta("10", EstadoCuenta.ACTIVA);
        assertThatThrownBy(() -> c.debitar(d("10.01"))).isInstanceOf(SaldoInsuficienteException.class);
        assertThat(c.saldo()).isEqualTo(d("10"));
    }

    @Test
    void debe_lanzar_cuenta_inactiva_al_verificar_activa() {
        assertThatThrownBy(() -> cuenta("10", EstadoCuenta.INACTIVA).verificarActiva())
                .isInstanceOf(CuentaInactivaException.class);
        cuenta("10", EstadoCuenta.ACTIVA).verificarActiva();
    }

    @Test
    void no_debe_exponer_titular_ni_numero_completo_en_toString() {
        assertThat(cuenta("10", EstadoCuenta.ACTIVA).toString()).doesNotContain("Ana").doesNotContain("0123456789");
    }
}
