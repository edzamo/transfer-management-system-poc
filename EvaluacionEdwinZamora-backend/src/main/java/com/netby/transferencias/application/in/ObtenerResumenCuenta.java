package com.netby.transferencias.application.in;

import com.netby.transferencias.application.dto.ResumenCuenta;

public interface ObtenerResumenCuenta {

    ResumenCuenta obtener(Query consulta);

    record Query(Long cuentaId) {
    }
}
