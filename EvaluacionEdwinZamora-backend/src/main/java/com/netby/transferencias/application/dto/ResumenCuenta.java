package com.netby.transferencias.application.dto;

import com.netby.transferencias.domain.dinero.Dinero;

public record ResumenCuenta(Long cuentaId, Dinero saldoActual, Dinero totalTransferidoHoy,
                            long cantidadTransferenciasHoy, long cantidadSospechosasHoy) {
}
