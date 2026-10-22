package com.netby.transferencias.infrastructure.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TransferenciaResourceTest {

    private static float saldo(long id) {
        return Api.resumen(id).then().statusCode(200).extract().jsonPath().getFloat("saldoActual");
    }

    @Test
    void debe_crear_transferencia_completada_con_201_y_mover_saldos() {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        long idO = Api.crearCuentaActiva(o, "1000");
        long idD = Api.crearCuentaActiva(d, "50");

        Api.transferir(o, d, "100.50").then().statusCode(201)
                .body("id", notNullValue())
                .body("cuentaOrigen", equalTo(o))
                .body("cuentaDestino", equalTo(d))
                .body("monto", equalTo(100.5f))
                .body("estado", equalTo("COMPLETADA"))
                .body("motivo", nullValue())
                .body("fecha", notNullValue());

        org.assertj.core.api.Assertions.assertThat(saldo(idO)).isEqualTo(899.5f);
        org.assertj.core.api.Assertions.assertThat(saldo(idD)).isEqualTo(150.5f);
    }

    @Test
    void debe_crear_en_revision_con_201_y_mover_saldos_cuando_supera_5000() {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        long idO = Api.crearCuentaActiva(o, "9000");
        long idD = Api.crearCuentaActiva(d, "0");

        Api.transferir(o, d, "5000.01").then().statusCode(201).body("estado", equalTo("EN_REVISION"));

        org.assertj.core.api.Assertions.assertThat(saldo(idO)).isEqualTo(3999.99f);
        org.assertj.core.api.Assertions.assertThat(saldo(idD)).isEqualTo(5000.01f);
    }

    @Test
    void debe_dar_400_para_datos_invalidos_y_no_registrar_nada() {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        Api.crearCuentaActiva(o, "1000");
        Api.crearCuentaActiva(d, "0");

        Api.transferir(o, d, "0").then().statusCode(400).body("code", equalTo("VALIDACION"))
                .body("details.field", hasItem("monto"));
        Api.transferir(o, d, "1.001").then().statusCode(400).body("code", equalTo("VALIDACION"));
        Api.transferir(o, d, "-5").then().statusCode(400);
        Api.transferir(o, o, "5").then().statusCode(400).body("details.field", hasItem("cuentaDestino"));
        Api.transferir("123", d, "5").then().statusCode(400);
        Api.postJson("/api/transferencias", "{}").then().statusCode(400)
                .body("details.field", hasItem("cuentaOrigen"))
                .body("details.field", hasItem("cuentaDestino"))
                .body("details.field", hasItem("monto"));

        given().queryParam("origen", o).when().get("/api/transferencias").then().statusCode(200)
                .body("totalElements", equalTo(0));
    }

    @Test
    void debe_dar_404_cuando_una_cuenta_no_existe_y_no_persistir() {
        String o = Api.numeroUnico();
        Api.crearCuentaActiva(o, "100");
        String inexistente = Api.numeroUnico();

        Api.transferir(o, inexistente, "5").then().statusCode(404).body("code", equalTo("CUENTA_NO_ENCONTRADA"));
        Api.transferir(inexistente, o, "5").then().statusCode(404).body("code", equalTo("CUENTA_NO_ENCONTRADA"));

        given().queryParam("origen", o).when().get("/api/transferencias").then().body("totalElements", equalTo(0));
        given().queryParam("origen", inexistente).when().get("/api/transferencias").then()
                .body("totalElements", equalTo(0));
    }

    @Test
    void debe_dar_422_por_cuenta_inactiva_y_persistir_rechazada_sin_mover_saldos() {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        long idO = Api.crearCuentaActiva(o, "100");
        Api.crearCuenta(d, "0", "INACTIVA").then().statusCode(201);

        Api.transferir(o, d, "10").then().statusCode(422).body("code", equalTo("CUENTA_INACTIVA"));

        org.assertj.core.api.Assertions.assertThat(saldo(idO)).isEqualTo(100f);
        given().queryParam("origen", o).queryParam("estado", "RECHAZADA").when().get("/api/transferencias").then()
                .body("totalElements", equalTo(1))
                .body("content[0].estado", equalTo("RECHAZADA"))
                .body("content[0].motivo", notNullValue());
    }

    @Test
    void debe_dar_422_por_saldo_insuficiente_y_registrar_rechazada() {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        long idO = Api.crearCuentaActiva(o, "10");
        Api.crearCuentaActiva(d, "0");

        Api.transferir(o, d, "10.01").then().statusCode(422).body("code", equalTo("SALDO_INSUFICIENTE"));

        org.assertj.core.api.Assertions.assertThat(saldo(idO)).isEqualTo(10f);
        given().queryParam("origen", o).when().get("/api/transferencias").then()
                .body("content[0].estado", equalTo("RECHAZADA"));
    }

    @Test
    void debe_dar_422_por_limite_diario_con_monto_restante() {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        Api.crearCuentaActiva(o, "50000");
        Api.crearCuentaActiva(d, "0");
        Api.transferir(o, d, "9999.99").then().statusCode(201);

        Api.transferir(o, d, "0.02").then().statusCode(422)
                .body("code", equalTo("LIMITE_DIARIO_EXCEDIDO"))
                .body("montoRestante", equalTo(0.01f))
                .body("message", org.hamcrest.Matchers.containsString("0.01"));
        Api.transferir(o, d, "0.01").then().statusCode(201); // llegar exactamente a 10000 está permitido
    }

    @Test
    void debe_consultar_historial_con_filtros_orden_descendente_y_paginacion() throws InterruptedException {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        Api.crearCuentaActiva(o, "1000");
        Api.crearCuentaActiva(d, "0");
        Api.transferir(o, d, "1").then().statusCode(201);
        Thread.sleep(5);
        Api.transferir(o, d, "2000").then().statusCode(422);
        Thread.sleep(5);
        Api.transferir(o, d, "3").then().statusCode(201);

        given().queryParam("origen", o).when().get("/api/transferencias").then().statusCode(200)
                .body("totalElements", equalTo(3))
                .body("content.monto", contains(3.0f, 2000.0f, 1.0f));
        given().queryParam("origen", o).queryParam("page", 1).queryParam("size", 2).when().get("/api/transferencias")
                .then().body("content", hasSize(1)).body("totalPages", equalTo(2));
        given().queryParam("origen", o).queryParam("destino", d).queryParam("estado", "COMPLETADA")
                .when().get("/api/transferencias").then().body("totalElements", equalTo(2));
        given().queryParam("origen", o).queryParam("desde", "2999-01-01T00:00:00Z").when().get("/api/transferencias")
                .then().body("totalElements", equalTo(0));
        given().queryParam("origen", o).queryParam("desde", "2000-01-01T00:00:00Z")
                .queryParam("hasta", "2999-01-01T00:00:00Z").when().get("/api/transferencias").then()
                .body("totalElements", equalTo(3));
    }

    @Test
    void debe_dar_400_para_filtros_invalidos_del_historial() {
        given().queryParam("origen", "abc").when().get("/api/transferencias").then().statusCode(400)
                .body("code", equalTo("VALIDACION"));
        given().queryParam("estado", "NOPE").when().get("/api/transferencias").then().statusCode(400);
        given().queryParam("desde", "ayer").when().get("/api/transferencias").then().statusCode(400);
        given().queryParam("desde", "2026-10-22T00:00:00Z").queryParam("hasta", "2026-10-21T00:00:00Z")
                .when().get("/api/transferencias").then().statusCode(400);
    }

    @Test
    void debe_dar_400_rapido_para_monto_con_exponente_enorme() {
        String o = Api.numeroUnico();
        String d = Api.numeroUnico();
        Api.crearCuentaActiva(o, "100");
        Api.crearCuentaActiva(d, "0");
        long inicio = System.nanoTime();
        Api.transferir(o, d, "1e999999999").then().statusCode(400).body("details.field", hasItem("monto"));
        org.assertj.core.api.Assertions.assertThat((System.nanoTime() - inicio) / 1_000_000L).isLessThan(2000L);
    }
}
