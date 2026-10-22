package com.netby.transferencias.domain.transferencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PeriodoTest {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil"); // UTC-5, sin DST

    private static Clock reloj(String instante) {
        return Clock.fixed(Instant.parse(instante), GUAYAQUIL);
    }

    @Test
    void dia_calendario_debe_ir_de_medianoche_local_a_medianoche_local_siguiente_en_utc() {
        Periodo p = Periodo.diaCalendario(reloj("2026-10-22T15:00:00Z")); // 10:00 local
        assertThat(p.desde()).isEqualTo(Instant.parse("2026-10-22T05:00:00Z"));
        assertThat(p.hasta()).isEqualTo(Instant.parse("2026-10-23T05:00:00Z"));
    }

    @Test
    void dia_calendario_debe_usar_el_dia_local_aunque_en_utc_ya_sea_otro() {
        Periodo p = Periodo.diaCalendario(reloj("2026-10-23T03:00:00Z")); // 22:00 del 22 local
        assertThat(p.desde()).isEqualTo(Instant.parse("2026-10-22T05:00:00Z"));
    }

    @Test
    void debe_ser_semiabierto() {
        Periodo p = Periodo.diaCalendario(reloj("2026-10-22T15:00:00Z"));
        assertThat(p.contiene(Instant.parse("2026-10-22T05:00:00Z"))).isTrue();
        assertThat(p.contiene(Instant.parse("2026-10-22T04:59:59.999999Z"))).isFalse();
        assertThat(p.contiene(Instant.parse("2026-10-23T04:59:59.999999Z"))).isTrue();
        assertThat(p.contiene(Instant.parse("2026-10-23T05:00:00Z"))).isFalse();
    }

    @Test
    void ultima_hora_debe_abarcar_desde_una_hora_antes_hasta_el_instante_actual_inclusive() {
        Periodo p = Periodo.ultimaHora(reloj("2026-10-22T15:00:00Z"));
        assertThat(p.contiene(Instant.parse("2026-10-22T14:00:00Z"))).isTrue();
        assertThat(p.contiene(Instant.parse("2026-10-22T13:59:59.999999Z"))).isFalse();
        assertThat(p.contiene(Instant.parse("2026-10-22T15:00:00Z"))).isTrue();
        assertThat(p.contiene(Instant.parse("2026-10-22T15:00:00.000001Z"))).isFalse();
    }
}
