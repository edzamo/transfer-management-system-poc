package com.netby.transferencias.adapter.in.rest;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.concurrent.ThreadLocalRandom;

/** Ayudante de pruebas HTTP. Cada prueba usa números de cuenta únicos para no depender de otras. */
final class Api {

    private Api() {
    }

    static String numeroUnico() {
        return String.format("%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }

    static Response postJson(String ruta, String cuerpo) {
        return given().contentType(ContentType.JSON).body(cuerpo).when().post(ruta);
    }

    static Response crearCuenta(String numero, String saldo, String estado) {
        return postJson("/api/cuentas", """
                {"numero":"%s","titular":"Ana Pérez","tipo":"AHORRO","saldoInicial":%s,"estado":"%s"}
                """.formatted(numero, saldo, estado));
    }

    static long crearCuentaActiva(String numero, String saldo) {
        return crearCuenta(numero, saldo, "ACTIVA").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    static Response transferir(String origen, String destino, String monto) {
        return postJson("/api/transferencias", """
                {"cuentaOrigen":"%s","cuentaDestino":"%s","monto":%s}
                """.formatted(origen, destino, monto));
    }

    static Response resumen(long id) {
        return given().when().get("/api/cuentas/" + id + "/resumen");
    }
}
