package com.netby.transferencias.application.in;


public interface ObtenerResumenCuenta {

    ResumenCuenta obtener(Query consulta);

    record Query(Long cuentaId) {
    }
}
