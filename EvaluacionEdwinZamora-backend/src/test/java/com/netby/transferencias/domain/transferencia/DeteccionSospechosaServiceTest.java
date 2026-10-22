package com.netby.transferencias.domain.transferencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.netby.transferencias.domain.dinero.Dinero;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DeteccionSospechosaServiceTest {

    private final DeteccionSospechosaService servicio = new DeteccionSospechosaService();

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    @Test
    void no_debe_ser_sospechosa_con_monto_exacto_de_5000() {
        assertThat(servicio.esSospechosa(d("5000.00"), 0)).isFalse();
    }

    @Test
    void debe_ser_sospechosa_con_monto_mayor_a_5000() {
        assertThat(servicio.esSospechosa(d("5000.01"), 0)).isTrue();
    }

    @Test
    void no_debe_ser_sospechosa_con_5_previas_en_la_ultima_hora() {
        assertThat(servicio.esSospechosa(d("10"), 4)).isFalse();
    }

    @Test
    void debe_ser_sospechosa_la_sexta_transferencia_de_la_hora() {
        assertThat(servicio.esSospechosa(d("10"), 5)).isTrue();
        assertThat(servicio.esSospechosa(d("10"), 6)).isTrue();
    }
}
