package com.netby.transferencias.infrastructure.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OpenApiContratoTest {

    private static String openapi() {
        return given().accept("application/json").when().get("/q/openapi").then().statusCode(200).extract().asString();
    }

    @Test
    void debe_documentar_el_tamano_maximo_200_y_no_100() {
        assertThat(openapi()).contains("1 a 200").doesNotContain("1 a 100");
    }

    @Test
    void debe_publicar_ejemplos_con_los_mensajes_reales() {
        String doc = openapi();
        assertThat(doc).doesNotContain("\"examples\" : { }")
                .contains("El campo saldoInicial no puede ser negativo")
                .doesNotContain("El saldo inicial no puede ser negativo")
                .doesNotContain("El campo monto debe ser mayor a cero");
        assertThat(doc.split("El monto debe ser mayor a cero", -1).length).isGreaterThan(2); // esquema + ejemplo
    }

    @Test
    void debe_declarar_el_schema_de_pagina_en_los_listados() {
        JsonPath doc = JsonPath.from(openapi());
        for (String ruta : new String[] {"/api/cuentas", "/api/transferencias"}) {
            String ref = doc.getString("paths.'" + ruta + "'.get.responses.'200'.content.'application/json'.schema.'$ref'");
            assertThat(ref).as(ruta).endsWith("PaginaResponse");
        }
    }
}
