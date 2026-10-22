package com.netby.transferencias.application.in;

import com.netby.transferencias.domain.shared.Pagina;
import com.netby.transferencias.domain.cuenta.Cuenta;

public interface ListarCuentas {

    Pagina<Cuenta> listar(Query consulta);

    record Query(int page, int size) {
    }
}
