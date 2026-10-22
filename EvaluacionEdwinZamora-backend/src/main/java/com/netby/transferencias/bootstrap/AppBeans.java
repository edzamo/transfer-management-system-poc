package com.netby.transferencias.bootstrap;

import com.netby.transferencias.domain.transferencia.DeteccionSospechosaService;
import com.netby.transferencias.domain.transferencia.LimiteDiarioService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.ZoneId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Composition root: cablea los objetos del dominio que no son beans CDI. */
@ApplicationScoped
public class AppBeans {

    @Produces
    @Singleton
    Clock reloj(@ConfigProperty(name = "app.timezone", defaultValue = "America/Guayaquil") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Produces
    @Singleton
    DeteccionSospechosaService deteccionSospechosa() {
        return new DeteccionSospechosaService();
    }

    @Produces
    @Singleton
    LimiteDiarioService limiteDiario() {
        return new LimiteDiarioService();
    }
}
