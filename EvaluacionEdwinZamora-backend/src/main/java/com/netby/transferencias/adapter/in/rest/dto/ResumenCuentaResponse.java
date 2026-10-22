package com.netby.transferencias.adapter.in.rest.dto;

import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Resumen de la cuenta para el día calendario actual")
public record ResumenCuentaResponse(
        @Schema(example = "1") Long cuentaId,
        @Schema(example = "1500.00") BigDecimal saldoActual,
        @Schema(description = "Suma de transferencias COMPLETADA y EN_REVISION de hoy", example = "600.00")
        BigDecimal totalTransferidoHoy,
        @Schema(description = "Cantidad de transferencias COMPLETADA y EN_REVISION de hoy", example = "3")
        long cantidadTransferenciasHoy,
        @Schema(description = "Cantidad de transferencias EN_REVISION de hoy", example = "1")
        long cantidadSospechosasHoy) {
}
