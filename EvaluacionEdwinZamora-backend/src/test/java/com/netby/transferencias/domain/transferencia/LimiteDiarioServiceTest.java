package com.netby.transferencias.domain.transferencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.LimiteDiarioExcedidoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LimiteDiarioServiceTest {

    private final LimiteDiarioService servicio = new LimiteDiarioService();

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    @Test
    void debe_permitir_llegar_exactamente_a_10000() {
        assertThatCode(() -> servicio.verificar(d("9999.99"), d("0.01"))).doesNotThrowAnyException();
        assertThatCode(() -> servicio.verificar(Dinero.CERO, d("10000"))).doesNotThrowAnyException();
    }

    @Test
    void debe_rechazar_informando_el_monto_restante() {
        assertThatThrownBy(() -> servicio.verificar(d("9999.99"), d("0.02")))
                .isInstanceOfSatisfying(LimiteDiarioExcedidoException.class, e -> {
                    assertThat(e.montoRestante()).isEqualByComparingTo("0.01");
                    assertThat(e.getMessage()).contains("0.01");
                    assertThat(e.code()).isEqualTo("LIMITE_DIARIO_EXCEDIDO");
                });
    }

    @Test
    void debe_informar_restante_cero_cuando_el_acumulado_ya_llego_al_limite() {
        assertThatThrownBy(() -> servicio.verificar(d("10000"), d("0.01")))
                .isInstanceOfSatisfying(LimiteDiarioExcedidoException.class,
                        e -> assertThat(e.montoRestante()).isEqualByComparingTo("0.00"));
    }

    @Test
    void debe_rechazar_un_monto_unico_mayor_al_limite() {
        assertThatThrownBy(() -> servicio.verificar(Dinero.CERO, d("10000.01")))
                .isInstanceOfSatisfying(LimiteDiarioExcedidoException.class,
                        e -> assertThat(e.montoRestante()).isEqualByComparingTo("10000.00"));
    }
}
