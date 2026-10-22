package com.netby.transferencias.adapter.in.rest.dto;

import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Solo valida presencia; las reglas de negocio (10 dígitos, saldo >= 0, ...) las aplica el dominio. */
@Schema(description = "Datos para registrar una cuenta bancaria")
public record CuentaRequest(
        @Schema(description = "Número único de 10 dígitos", example = "1234567890") @NotBlank String numero,
        @Schema(description = "Titular de la cuenta (máx. 150 caracteres)", example = "Ana Pérez") @NotBlank String titular,
        @Schema(description = "Tipo de cuenta", example = "AHORRO") @NotNull TipoCuenta tipo,
        @Schema(description = "Saldo inicial (>= 0, máx. 2 decimales)", example = "1500.00") @NotNull BigDecimal saldoInicial,
        @Schema(description = "Estado de la cuenta", example = "ACTIVA") @NotNull EstadoCuenta estado) {
}
