package com.netby.transferencias.infrastructure.adapter.in.rest.dto;

import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Cuenta bancaria")
public record CuentaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1234567890") String numero,
        @Schema(example = "Ana Pérez") String titular,
        @Schema(example = "AHORRO") TipoCuenta tipo,
        @Schema(example = "1500.00") BigDecimal saldo,
        @Schema(example = "ACTIVA") EstadoCuenta estado) {
}
