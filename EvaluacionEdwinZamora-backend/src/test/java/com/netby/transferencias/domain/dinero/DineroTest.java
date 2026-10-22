package com.netby.transferencias.domain.dinero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.domain.error.DatosInvalidosException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DineroTest {

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    @Test
    void debe_normalizar_a_escala_2_cuando_trae_menos_decimales() {
        assertThat(d("10").valor()).isEqualByComparingTo("10.00");
        assertThat(d("10").valor().scale()).isEqualTo(2);
        assertThat(d("10.5")).isEqualTo(d("10.50"));
    }

    @Test
    void debe_aceptar_ceros_a_la_derecha_mas_alla_de_2_decimales() {
        assertThat(d("10.500").valor().scale()).isEqualTo(2);
    }

    @Test
    void debe_rechazar_mas_de_2_decimales_sin_redondear() {
        assertThatThrownBy(() -> d("10.001")).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_rechazar_exponentes_enormes_rapidamente() {
        assertThatThrownBy(() -> d("1e999999999")).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> d("1e30")).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> d("1e-999999999")).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_respetar_el_limite_decimal_19_2() {
        assertThat(d("99999999999999999.99").valor()).isEqualByComparingTo("99999999999999999.99");
        assertThatThrownBy(() -> d("100000000000000000.00")).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> d("-100000000000000000.00")).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_rechazar_negativo_y_nulo() {
        assertThatThrownBy(() -> d("-0.01")).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Dinero.de(null)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_permitir_cero_pero_no_considerarlo_positivo() {
        assertThat(Dinero.CERO.esPositivo()).isFalse();
        assertThat(d("0.01").esPositivo()).isTrue();
        assertThat(d("0")).isEqualTo(Dinero.CERO);
    }

    @Test
    void debe_sumar_restar_y_comparar() {
        assertThat(d("9999.99").sumar(d("0.01"))).isEqualTo(d("10000"));
        assertThat(d("10").restar(d("0.01"))).isEqualTo(d("9.99"));
        assertThat(d("5000.01").esMayorQue(d("5000"))).isTrue();
        assertThat(d("5000").esMayorQue(d("5000.00"))).isFalse();
    }

    @Test
    void debe_rechazar_resta_que_deja_negativo() {
        assertThatThrownBy(() -> d("1").restar(d("1.01"))).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_informar_el_campo_que_origino_el_error() {
        assertThatThrownBy(() -> Dinero.de(new BigDecimal("-1"), "saldoInicial"))
                .isInstanceOfSatisfying(DatosInvalidosException.class, e -> assertThat(e.campo()).isEqualTo("saldoInicial"));
        assertThatThrownBy(() -> Dinero.de(new BigDecimal("1.234"), "monto"))
                .isInstanceOfSatisfying(DatosInvalidosException.class, e -> assertThat(e.campo()).isEqualTo("monto"));
    }
}
