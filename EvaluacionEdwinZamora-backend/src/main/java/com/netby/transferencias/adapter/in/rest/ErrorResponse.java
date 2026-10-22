package com.netby.transferencias.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Formato único de error de la API (D5). */
@Schema(description = "Error de la API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(example = "2026-10-22T15:00:00Z") Instant timestamp,
        @Schema(example = "422") int status,
        @Schema(description = "Código estable para el cliente", example = "SALDO_INSUFICIENTE") String code,
        @Schema(example = "Saldo insuficiente en la cuenta ******7890") String message,
        @Schema(example = "/api/transferencias") String path,
        @Schema(description = "Detalle por campo (errores de validación)") List<Detalle> details,
        @Schema(description = "Solo en LIMITE_DIARIO_EXCEDIDO: monto aún disponible hoy", example = "500.00")
        BigDecimal montoRestante) {

    public record Detalle(@Schema(example = "monto") String field, @Schema(example = "El monto debe ser mayor a cero") String message) {
    }
}
