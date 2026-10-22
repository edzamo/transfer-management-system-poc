package com.netby.transferencias.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Datos para ejecutar una transferencia")
public record TransferenciaRequest(
        @Schema(description = "Número de la cuenta origen", example = "1234567890") @NotBlank String cuentaOrigen,
        @Schema(description = "Número de la cuenta destino", example = "0987654321") @NotBlank String cuentaDestino,
        @Schema(description = "Monto > 0 con máximo 2 decimales", example = "250.75") @NotNull BigDecimal monto) {
}
