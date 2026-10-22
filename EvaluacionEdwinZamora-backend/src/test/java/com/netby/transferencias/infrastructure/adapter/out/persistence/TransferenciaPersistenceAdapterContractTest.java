package com.netby.transferencias.infrastructure.adapter.out.persistence;

import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.contract.TransferenciaRepositoryContract;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class TransferenciaPersistenceAdapterContractTest extends TransferenciaRepositoryContract {

    @Inject
    TransferenciaRepository repositorio;

    @Override
    protected TransferenciaRepository repositorio() {
        return repositorio;
    }
}
