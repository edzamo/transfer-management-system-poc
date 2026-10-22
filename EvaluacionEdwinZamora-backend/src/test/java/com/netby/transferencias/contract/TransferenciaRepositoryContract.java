package com.netby.transferencias.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.netby.transferencias.application.dto.FiltroTransferencias;
import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.Periodo;
import com.netby.transferencias.domain.transferencia.Transferencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class TransferenciaRepositoryContract {

    private static final Instant BASE = Instant.parse("2026-10-22T10:00:00Z");

    protected abstract TransferenciaRepository repositorio();

    private static NumeroCuenta numeroUnico() {
        return NumeroCuenta.de(String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L)));
    }

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    private Transferencia guardar(NumeroCuenta o, NumeroCuenta dst, String monto, EstadoTransferencia e, Instant f) {
        Transferencia t = switch (e) {
            case COMPLETADA -> Transferencia.completada(o, dst, d(monto), f);
            case EN_REVISION -> Transferencia.enRevision(o, dst, d(monto), f);
            case RECHAZADA -> Transferencia.rechazada(o, dst, d(monto), f, "motivo de prueba");
        };
        return repositorio().guardar(t);
    }

    @ParameterizedTest
    @EnumSource(EstadoTransferencia.class)
    void debe_conservar_cada_estado_en_ida_y_vuelta(EstadoTransferencia estado) {
        NumeroCuenta o = numeroUnico();
        NumeroCuenta dst = numeroUnico();
        Instant fecha = BASE.plusNanos(123_456_000); // precisión de microsegundos

        Transferencia guardada = guardar(o, dst, "12.34", estado, fecha);

        assertThat(guardada.id()).isNotNull();
        var leida = repositorio().buscar(new FiltroTransferencias(o, null, null, null, null), 0, 10).contenido();
        assertThat(leida).hasSize(1);
        Transferencia t = leida.get(0);
        assertThat(t.id()).isEqualTo(guardada.id());
        assertThat(t.estado()).isEqualTo(estado);
        assertThat(t.cuentaOrigen()).isEqualTo(o);
        assertThat(t.cuentaDestino()).isEqualTo(dst);
        assertThat(t.monto()).isEqualTo(d("12.34"));
        assertThat(t.fecha()).isEqualTo(fecha);
        assertThat(t.motivo()).isEqualTo(estado == EstadoTransferencia.RECHAZADA ? "motivo de prueba" : null);
    }

    @Test
    void debe_contar_y_sumar_por_origen_estados_y_periodo_semiabierto() {
        NumeroCuenta o = numeroUnico();
        NumeroCuenta dst = numeroUnico();
        Periodo periodo = new Periodo(BASE, BASE.plus(1, ChronoUnit.HOURS));
        guardar(o, dst, "10", EstadoTransferencia.COMPLETADA, BASE);                                // borde inferior: dentro
        guardar(o, dst, "20", EstadoTransferencia.EN_REVISION, BASE.plus(30, ChronoUnit.MINUTES));
        guardar(o, dst, "40", EstadoTransferencia.RECHAZADA, BASE.plus(30, ChronoUnit.MINUTES));       // otro estado
        guardar(o, dst, "80", EstadoTransferencia.COMPLETADA, BASE.plus(1, ChronoUnit.HOURS));         // borde superior: fuera
        guardar(o, dst, "160", EstadoTransferencia.COMPLETADA, BASE.minusNanos(1_000));                // antes: fuera
        guardar(dst, o, "320", EstadoTransferencia.COMPLETADA, BASE.plus(1, ChronoUnit.MINUTES));      // otro origen

        Set<EstadoTransferencia> estados = EstadoTransferencia.CONSUMEN_CUPO;
        assertThat(repositorio().contar(o, estados, periodo)).isEqualTo(2);
        assertThat(repositorio().sumar(o, estados, periodo)).isEqualTo(d("30"));
        assertThat(repositorio().contar(o, Set.of(EstadoTransferencia.EN_REVISION), periodo)).isEqualTo(1);
    }

    @Test
    void debe_sumar_cero_cuando_no_hay_transferencias() {
        Periodo periodo = new Periodo(BASE, BASE.plus(1, ChronoUnit.HOURS));
        assertThat(repositorio().sumar(numeroUnico(), EstadoTransferencia.CONSUMEN_CUPO, periodo)).isEqualTo(Dinero.CERO);
        assertThat(repositorio().contar(numeroUnico(), EstadoTransferencia.CONSUMEN_CUPO, periodo)).isZero();
    }

    @Test
    void debe_buscar_con_filtros_orden_por_fecha_descendente_y_paginacion() {
        NumeroCuenta o = numeroUnico();
        NumeroCuenta dst = numeroUnico();
        guardar(o, dst, "1", EstadoTransferencia.COMPLETADA, BASE.plusSeconds(10));
        guardar(o, dst, "2", EstadoTransferencia.RECHAZADA, BASE.plusSeconds(30));
        guardar(o, dst, "3", EstadoTransferencia.COMPLETADA, BASE.plusSeconds(20));
        guardar(o, numeroUnico(), "4", EstadoTransferencia.COMPLETADA, BASE.plusSeconds(40));

        var porOrigen = repositorio().buscar(new FiltroTransferencias(o, null, null, null, null), 0, 2);
        assertThat(porOrigen.contenido()).extracting(t -> t.monto().valor().intValue()).containsExactly(4, 2);
        assertThat(porOrigen.totalElements()).isEqualTo(4);
        assertThat(repositorio().buscar(new FiltroTransferencias(o, null, null, null, null), 1, 2).contenido())
                .extracting(t -> t.monto().valor().intValue()).containsExactly(3, 1);

        assertThat(repositorio().buscar(new FiltroTransferencias(o, dst, null, null, null), 0, 10).totalElements())
                .isEqualTo(3);
        assertThat(repositorio().buscar(new FiltroTransferencias(o, dst, null, null, EstadoTransferencia.RECHAZADA), 0, 10)
                .contenido()).hasSize(1);
        var rango = repositorio().buscar(
                new FiltroTransferencias(o, dst, BASE.plusSeconds(20), BASE.plusSeconds(30), null), 0, 10);
        assertThat(rango.contenido()).extracting(t -> t.monto().valor().intValue()).containsExactly(2, 3);
    }
}
