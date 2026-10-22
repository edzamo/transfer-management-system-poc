package com.netby.transferencias.domain.transferencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.error.DatosInvalidosException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransferenciaTest {

    private static final NumeroCuenta A = NumeroCuenta.de("0000000001");
    private static final NumeroCuenta B = NumeroCuenta.de("0000000002");
    private static final Instant FECHA = Instant.parse("2026-10-22T15:00:00Z");

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    @Test
    void debe_crear_completada_sin_id_ni_motivo() {
        Transferencia t = Transferencia.completada(A, B, d("10"), FECHA);
        assertThat(t.estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
        assertThat(t.id()).isNull();
        assertThat(t.motivo()).isNull();
        assertThat(t.cuentaOrigen()).isEqualTo(A);
        assertThat(t.cuentaDestino()).isEqualTo(B);
        assertThat(t.monto()).isEqualTo(d("10"));
        assertThat(t.fecha()).isEqualTo(FECHA);
    }

    @Test
    void debe_crear_en_revision() {
        assertThat(Transferencia.enRevision(A, B, d("6000"), FECHA).estado()).isEqualTo(EstadoTransferencia.EN_REVISION);
    }

    @Test
    void debe_crear_rechazada_con_motivo() {
        Transferencia t = Transferencia.rechazada(A, B, d("10"), FECHA, "Saldo insuficiente");
        assertThat(t.estado()).isEqualTo(EstadoTransferencia.RECHAZADA);
        assertThat(t.motivo()).isEqualTo("Saldo insuficiente");
    }

    @Test
    void debe_exigir_motivo_en_rechazada() {
        assertThatThrownBy(() -> Transferencia.rechazada(A, B, d("10"), FECHA, " "))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Transferencia.rechazada(A, B, d("10"), FECHA, null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_rechazar_monto_cero() {
        assertThatThrownBy(() -> Transferencia.completada(A, B, Dinero.CERO, FECHA))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_rechazar_origen_igual_a_destino() {
        assertThatThrownBy(() -> Transferencia.completada(A, A, d("10"), FECHA))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_rechazar_datos_nulos() {
        assertThatThrownBy(() -> Transferencia.completada(null, B, d("10"), FECHA))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Transferencia.completada(A, B, null, FECHA))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Transferencia.completada(A, B, d("10"), null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void debe_reconstruir_con_id_y_aplicar_las_mismas_invariantes() {
        Transferencia t = Transferencia.reconstruir(5L, A, B, d("10"), EstadoTransferencia.RECHAZADA, "x", FECHA);
        assertThat(t.id()).isEqualTo(5L);
        assertThatThrownBy(() -> Transferencia.reconstruir(5L, A, B, d("10"), EstadoTransferencia.COMPLETADA, "x", FECHA))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Transferencia.reconstruir(5L, A, B, d("10"), EstadoTransferencia.RECHAZADA, null, FECHA))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> Transferencia.reconstruir(5L, A, B, d("10"), null, null, FECHA))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void solo_completada_y_en_revision_consumen_cupo() {
        assertThat(EstadoTransferencia.CONSUMEN_CUPO)
                .containsExactlyInAnyOrder(EstadoTransferencia.COMPLETADA, EstadoTransferencia.EN_REVISION);
    }
}
