package com.netby.transferencias.application.fake;

import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.contract.CuentaRepositoryContract;

class InMemoryCuentaRepositoryContractTest extends CuentaRepositoryContract {

    private final CuentaRepository repositorio = new InMemoryCuentaRepository();

    @Override
    protected CuentaRepository repositorio() {
        return repositorio;
    }
}
