package com.netby.transferencias.application.in;

import com.netby.transferencias.application.dto.Pagina;
import com.netby.transferencias.domain.cuenta.Cuenta;

public interface ListarCuentas {

    Pagina<Cuenta> listar(Query consulta);

    record Query(int page, int size) {
    }
}
