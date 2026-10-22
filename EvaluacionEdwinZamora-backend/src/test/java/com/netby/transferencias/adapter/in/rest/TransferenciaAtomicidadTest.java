package com.netby.transferencias.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.netby.transferencias.application.out.CuentaRepository;
import com.netby.transferencias.application.out.TransferenciaRepository;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Prueba de rollback real: un fallo técnico tras actualizar saldos no deja estado parcial en MySQL. */
@QuarkusTest
class TransferenciaAtomicidadTest {

    @Inject
    CuentaRepository cuentas;

    @InjectSpy
    TransferenciaRepository transferencias;

    @Test
    void debe_revertir_saldos_y_registro_cuando_falla_el_ultimo_paso_de_la_transaccion() {
        String origen = Api.numeroUnico();
        String destino = Api.numeroUnico();
        long idOrigen = Api.crearCuentaActiva(origen, "1000");
        long idDestino = Api.crearCuentaActiva(destino, "0");
        AtomicReference<Object> saldoVistoDentro = new AtomicReference<>();

        doAnswer(invocacion -> {
            invocacion.callRealMethod();                                   // el INSERT ya se ejecutó
            // Esta consulta fuerza el flush de los UPDATE de saldo pendientes: ya están en la base, sin commit.
            saldoVistoDentro.set(cuentas.buscarPorNumero(NumeroCuenta.de(origen)).orElseThrow().saldo().valor());
            throw new IllegalStateException("fallo simulado tras escribir");
        }).when(transferencias).guardar(any());

        Api.transferir(origen, destino, "100").then().statusCode(500).body("code", equalTo("ERROR_INTERNO"));

        assertThat(saldoVistoDentro.get()).hasToString("900.00");           // dentro de la tx sí estaba movido
        assertThat(Api.resumen(idOrigen).jsonPath().getFloat("saldoActual")).isEqualTo(1000f);
        assertThat(Api.resumen(idDestino).jsonPath().getFloat("saldoActual")).isEqualTo(0f);
        given().queryParam("origen", origen).when().get("/api/transferencias").then().body("totalElements", equalTo(0));
    }
}
