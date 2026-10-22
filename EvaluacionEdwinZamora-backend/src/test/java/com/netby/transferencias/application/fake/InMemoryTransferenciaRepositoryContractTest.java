package com.netby.transferencias.application.fake;

import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.contract.TransferenciaRepositoryContract;

class InMemoryTransferenciaRepositoryContractTest extends TransferenciaRepositoryContract {

    private final TransferenciaRepository repositorio = new InMemoryTransferenciaRepository();

    @Override
    protected TransferenciaRepository repositorio() {
        return repositorio;
    }
}
