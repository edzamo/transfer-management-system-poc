package com.netby.transferencias.application.in;

import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import java.math.BigDecimal;

public interface RegistrarCuenta {

    Cuenta registrar(Command comando);

    record Command(String numero, String titular, TipoCuenta tipo, BigDecimal saldoInicial, EstadoCuenta estado) {
    }
}
