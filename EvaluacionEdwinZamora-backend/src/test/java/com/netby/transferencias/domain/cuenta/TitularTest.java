package com.netby.transferencias.domain.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.domain.error.DatosInvalidosException;
import org.junit.jupiter.api.Test;

class TitularTest {

    @Test
    void debe_recortar_espacios_y_aceptar_150_caracteres() {
        assertThat(Titular.de("  Ana Pérez ").valor()).isEqualTo("Ana Pérez");
        assertThat(Titular.de("a".repeat(150)).valor()).hasSize(150);
    }

    @Test
    void debe_rechazar_nulo_vacio_o_mas_de_150() {
        assertThatThrownBy(() -> Titular.de(null)).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Titular.de("   ")).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Titular.de("a".repeat(151))).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void no_debe_exponer_el_nombre_en_toString() {
        assertThat(Titular.de("Ana Pérez").toString()).doesNotContain("Ana");
    }
}
