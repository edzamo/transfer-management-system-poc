package com.netby.transferencias.domain.transferencia;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/** Intervalo semiabierto de instantes UTC: [desde, hasta). */
public record Periodo(Instant desde, Instant hasta) {

    private static final Duration VENTANA_SOSPECHA = Duration.ofHours(1);

    public Periodo {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Periodo inválido");
        }
    }

    /** Día calendario según la zona del reloj, expresado en instantes UTC. */
    public static Periodo diaCalendario(Clock reloj) {
        LocalDate hoy = LocalDate.now(reloj);
        return new Periodo(
                hoy.atStartOfDay(reloj.getZone()).toInstant(),
                hoy.plusDays(1).atStartOfDay(reloj.getZone()).toInstant());
    }

    /** Desde una hora antes hasta el instante actual inclusive (por eso el límite abierto es ahora + 1 ns). */
    public static Periodo ultimaHora(Clock reloj) {
        Instant ahora = reloj.instant();
        return new Periodo(ahora.minus(VENTANA_SOSPECHA), ahora.plusNanos(1));
    }

    public boolean contiene(Instant instante) {
        return !instante.isBefore(desde) && instante.isBefore(hasta);
    }
}
