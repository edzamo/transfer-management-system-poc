package com.netby.transferencias.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TransferenciaConcurrenciaTest {

    private static List<Response> ejecutarALaVez(List<Callable<Response>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Response>> futuros = new ArrayList<>();
            for (Callable<Response> tarea : tareas) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            List<Response> respuestas = new ArrayList<>();
            for (Future<Response> f : futuros) {
                respuestas.add(f.get(60, TimeUnit.SECONDS));
            }
            return respuestas;
        } finally {
            pool.shutdownNow();
        }
    }

    private static BigDecimal saldo(long id) {
        return new BigDecimal(Api.resumen(id).then().statusCode(200).extract().jsonPath().getString("saldoActual"));
    }

    @Test
    void debe_resolver_transferencias_opuestas_A_a_B_y_B_a_A_sin_deadlock_ni_saldo_negativo() throws Exception {
        String a = Api.numeroUnico();
        String b = Api.numeroUnico();
        long idA = Api.crearCuentaActiva(a, "500");
        long idB = Api.crearCuentaActiva(b, "500");
        List<Callable<Response>> tareas = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            tareas.add(() -> Api.transferir(a, b, "3"));
            tareas.add(() -> Api.transferir(b, a, "2"));
        }

        List<Response> respuestas = ejecutarALaVez(tareas);

        assertThat(respuestas).extracting(Response::statusCode).containsOnly(201); // ni 409 ni 500: sin deadlock
        assertThat(saldo(idA)).isEqualByComparingTo("485.00");   // 500 - 15*3 + 15*2
        assertThat(saldo(idB)).isEqualByComparingTo("515.00");
    }

    @Test
    void debe_impedir_que_dos_hilos_superen_el_limite_diario_de_10000() throws Exception {
        String origen = Api.numeroUnico();
        String destino = Api.numeroUnico();
        long idOrigen = Api.crearCuentaActiva(origen, "50000");
        Api.crearCuentaActiva(destino, "0");
        List<Callable<Response>> tareas = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tareas.add(() -> Api.transferir(origen, destino, "1000"));
        }

        List<Response> respuestas = ejecutarALaVez(tareas);

        long exitosas = respuestas.stream().filter(r -> r.statusCode() == 201).count();
        long rechazadas = respuestas.stream().filter(r -> r.statusCode() == 422).count();
        assertThat(exitosas).isEqualTo(10);
        assertThat(rechazadas).isEqualTo(2);
        assertThat(respuestas).filteredOn(r -> r.statusCode() == 422)
                .allSatisfy(r -> assertThat(r.jsonPath().getString("code")).isEqualTo("LIMITE_DIARIO_EXCEDIDO"));
        assertThat(saldo(idOrigen)).isEqualByComparingTo("40000.00");
    }

    @Test
    void debe_permitir_solo_una_transferencia_grande_cuando_dos_hilos_compiten_por_el_cupo() throws Exception {
        String origen = Api.numeroUnico();
        String destino = Api.numeroUnico();
        Api.crearCuentaActiva(origen, "50000");
        Api.crearCuentaActiva(destino, "0");

        List<Response> respuestas = ejecutarALaVez(List.of(
                () -> Api.transferir(origen, destino, "6000"),
                () -> Api.transferir(origen, destino, "6000")));

        assertThat(respuestas).extracting(Response::statusCode).containsExactlyInAnyOrder(201, 422);
    }
}
