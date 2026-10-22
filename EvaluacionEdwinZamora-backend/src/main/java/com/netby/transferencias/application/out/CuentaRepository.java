package com.netby.transferencias.application.out;

import com.netby.transferencias.application.dto.Pagina;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import java.util.Optional;

public interface CuentaRepository {

    Cuenta guardar(Cuenta cuenta);

    boolean existePorNumero(NumeroCuenta numero);

    Optional<Cuenta> buscarPorNumero(NumeroCuenta numero);

    Optional<Cuenta> buscarPorId(Long id);

    /** Bloqueo de escritura sobre la cuenta; solo válido dentro de una transacción. */
    Optional<Cuenta> buscarPorNumeroParaActualizar(NumeroCuenta numero);

    Pagina<Cuenta> listar(int page, int size);
}
