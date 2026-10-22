package com.netby.transferencias.domain.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.domain.error.DatosInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class NumeroCuentaTest {

    @Test
    void debe_aceptar_exactamente_10_digitos() {
        assertThat(NumeroCuenta.de("0123456789").valor()).isEqualTo("0123456789");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "123456789", "12345678901", "12345abcde", "1234 56789", " 123456789"})
    void debe_rechazar_numero_que_no_son_10_digitos(String valor) {
        assertThatThrownBy(() -> NumeroCuenta.de(valor)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_enmascarar_dejando_los_ultimos_4() {
        assertThat(NumeroCuenta.de("0123456789").enmascarado()).isEqualTo("******6789");
    }
}
