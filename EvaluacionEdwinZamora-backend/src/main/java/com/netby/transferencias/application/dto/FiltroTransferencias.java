package com.netby.transferencias.application.dto;

import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import java.time.Instant;

/** Todos los criterios son opcionales (nulos = sin filtrar). Rango de fechas inclusivo. */
public record FiltroTransferencias(NumeroCuenta origen, NumeroCuenta destino, Instant desde, Instant hasta,
                                   EstadoTransferencia estado) {
}
