package com.netby.transferencias.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.contract.CuentaRepositoryContract;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import com.netby.transferencias.domain.error.CuentaBloqueadaException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CuentaPersistenceAdapterContractTest extends CuentaRepositoryContract {

    @Inject
    CuentaRepository repositorio;

    @Override
    protected CuentaRepository repositorio() {
        return repositorio;
    }

    @Override
    protected <T> T enTransaccion(Supplier<T> accion) {
        return QuarkusTransaction.requiringNew().call(accion::get);
    }

    @Test
    void debe_traducir_el_timeout_de_bloqueo_a_cuenta_bloqueada() throws Exception {
        NumeroCuenta numero = numeroUnico();
        Cuenta c = nueva(numero, "5", TipoCuenta.AHORRO, EstadoCuenta.ACTIVA);
        CountDownLatch bloqueada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        Thread retenedor = Thread.ofPlatform().start(() -> QuarkusTransaction.requiringNew().run(() -> {
            repositorio.buscarPorNumeroParaActualizar(numero);
            bloqueada.countDown();
            try {
                liberar.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertThat(bloqueada.await(10, TimeUnit.SECONDS)).isTrue();
        try {
            assertThatThrownBy(() -> QuarkusTransaction.requiringNew()
                    .run(() -> repositorio.buscarPorNumeroParaActualizar(numero)))
                    .isInstanceOf(CuentaBloqueadaException.class);
        } finally {
            liberar.countDown();
            retenedor.join();
        }
        assertThat(c.id()).isNotNull();
    }
}
