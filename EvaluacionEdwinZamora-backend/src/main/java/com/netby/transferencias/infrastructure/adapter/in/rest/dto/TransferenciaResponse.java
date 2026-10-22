package com.netby.transferencias.infrastructure.adapter.in.rest.dto;

import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import java.math.BigDecimal;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Transferencia registrada (COMPLETADA, EN_REVISION o RECHAZADA)")
public record TransferenciaResponse(
        @Schema(example = "10") Long id,
        @Schema(example = "1234567890") String cuentaOrigen,
        @Schema(example = "0987654321") String cuentaDestino,
        @Schema(example = "250.75") BigDecimal monto,
        @Schema(example = "COMPLETADA") EstadoTransferencia estado,
        @Schema(description = "Motivo del rechazo; nulo si no fue rechazada", nullable = true) String motivo,
        @Schema(description = "Instante UTC (ISO-8601)", example = "2026-10-22T15:00:00Z") Instant fecha) {
}
