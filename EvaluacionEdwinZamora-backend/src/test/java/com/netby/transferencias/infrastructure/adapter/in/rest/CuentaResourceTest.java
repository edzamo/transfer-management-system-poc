package com.netby.transferencias.infrastructure.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CuentaResourceTest {

    @Test
    void debe_registrar_cuenta_con_201_e_id() {
        String numero = Api.numeroUnico();

        Api.crearCuenta(numero, "100.5", "ACTIVA").then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("numero", equalTo(numero))
                .body("titular", equalTo("Ana Pérez"))
                .body("tipo", equalTo("AHORRO"))
                .body("saldo", equalTo(100.5f))
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    void debe_dar_409_con_formato_de_error_unico_cuando_el_numero_esta_duplicado() {
        String numero = Api.numeroUnico();
        Api.crearCuentaActiva(numero, "1");

        Api.crearCuenta(numero, "2", "ACTIVA").then()
                .statusCode(409)
                .body("code", equalTo("CUENTA_DUPLICADA"))
                .body("status", equalTo(409))
                .body("path", equalTo("/api/cuentas"))
                .body("timestamp", notNullValue())
                .body("message", notNullValue())
                .body("details", empty())
                .body("message", not(containsString(numero)));
    }

    @Test
    void debe_dar_400_atribuyendo_el_saldo_negativo_a_saldoInicial() {
        Api.crearCuenta(Api.numeroUnico(), "-1", "ACTIVA").then()
                .statusCode(400)
                .body("code", equalTo("VALIDACION"))
                .body("details.field", hasItem("saldoInicial"));
    }

    @Test
    void debe_dar_400_para_numero_que_no_tiene_10_digitos() {
        Api.crearCuenta("12345", "1", "ACTIVA").then()
                .statusCode(400)
                .body("code", equalTo("VALIDACION"))
                .body("details.field", hasItem("numero"));
    }

    @Test
    void debe_dar_400_con_detalle_por_campo_cuando_faltan_datos_obligatorios() {
        Api.postJson("/api/cuentas", "{}").then()
                .statusCode(400)
                .body("code", equalTo("VALIDACION"))
                .body("details.field", hasItem("numero"))
                .body("details.field", hasItem("titular"))
                .body("details.field", hasItem("tipo"))
                .body("details.field", hasItem("saldoInicial"))
                .body("details.field", hasItem("estado"));
    }

    @Test
    void debe_dar_400_para_json_malformado_o_enum_desconocido() {
        Api.postJson("/api/cuentas", "{no es json").then().statusCode(400).body("code", equalTo("VALIDACION"));
        Api.postJson("/api/cuentas", """
                {"numero":"%s","titular":"A","tipo":"VIP","saldoInicial":1,"estado":"ACTIVA"}
                """.formatted(Api.numeroUnico())).then().statusCode(400).body("code", equalTo("VALIDACION"));
    }

    @Test
    void debe_listar_paginado_con_la_estructura_de_pagina() {
        for (int i = 0; i < 3; i++) {
            Api.crearCuentaActiva(Api.numeroUnico(), "1");
        }

        given().queryParam("page", 0).queryParam("size", 2).when().get("/api/cuentas").then()
                .statusCode(200)
                .body("content", hasSize(2))
                .body("page", equalTo(0))
                .body("size", equalTo(2))
                .body("totalElements", greaterThanOrEqualTo(3))
                .body("totalPages", greaterThanOrEqualTo(2))
                .body("content[0].id", notNullValue());
        given().when().get("/api/cuentas").then().statusCode(200).body("size", equalTo(20))
                .body("content.size()", lessThanOrEqualTo(20));
    }

    @Test
    void debe_dar_400_para_paginacion_invalida() {
        given().queryParam("size", 0).when().get("/api/cuentas").then().statusCode(400).body("code", equalTo("VALIDACION"));
        given().queryParam("page", -1).when().get("/api/cuentas").then().statusCode(400);
    }

    @Test
    void debe_aceptar_size_200_y_rechazar_201() {
        given().queryParam("size", 200).when().get("/api/cuentas").then().statusCode(200).body("size", equalTo(200));
        given().queryParam("size", 201).when().get("/api/cuentas").then().statusCode(400).body("code", equalTo("VALIDACION"));
    }

    @Test
    void debe_dar_400_para_saldo_inicial_fuera_de_rango_decimal_19_2() {
        Api.crearCuenta(Api.numeroUnico(), "100000000000000000.00", "ACTIVA").then().statusCode(400)
                .body("details.field", hasItem("saldoInicial"));
        Api.crearCuenta(Api.numeroUnico(), "1e999999999", "ACTIVA").then().statusCode(400)
                .body("details.field", hasItem("saldoInicial"));
    }

    @Test
    void debe_dar_400_para_parametro_de_consulta_no_numerico() {
        given().queryParam("page", "abc").when().get("/api/cuentas").then().statusCode(400)
                .body("code", equalTo("VALIDACION"));
    }

    @Test
    void debe_dar_404_con_codigo_estable_para_resumen_de_cuenta_inexistente() {
        Api.resumen(999_999_999L).then().statusCode(404)
                .body("code", equalTo("CUENTA_NO_ENCONTRADA"))
                .body("path", equalTo("/api/cuentas/999999999/resumen"));
    }

    @Test
    void debe_resumir_saldo_total_y_conteos_del_dia() {
        String origen = Api.numeroUnico();
        String destino = Api.numeroUnico();
        long idOrigen = Api.crearCuentaActiva(origen, "20000");
        Api.crearCuentaActiva(destino, "0");
        Api.transferir(origen, destino, "100.25").then().statusCode(201);
        Api.transferir(origen, destino, "6000").then().statusCode(201); // EN_REVISION
        Api.transferir(origen, destino, "5000").then().statusCode(422); // límite: no cuenta en el resumen

        Api.resumen(idOrigen).then().statusCode(200)
                .body("cuentaId", equalTo((int) idOrigen))
                .body("saldoActual", equalTo(13899.75f))
                .body("totalTransferidoHoy", equalTo(6100.25f))
                .body("cantidadTransferenciasHoy", equalTo(2))
                .body("cantidadSospechosasHoy", equalTo(1));
    }
}
