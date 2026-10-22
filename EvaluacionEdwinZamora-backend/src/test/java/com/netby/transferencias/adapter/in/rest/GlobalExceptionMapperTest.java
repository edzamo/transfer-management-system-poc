package com.netby.transferencias.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

class GlobalExceptionMapperTest {

    @Test
    void debe_responder_500_generico_sin_filtrar_detalles_internos() {
        GlobalExceptionMapper mapper = new GlobalExceptionMapper();
        mapper.uriInfo = mock(UriInfo.class);
        when(mapper.uriInfo.getPath()).thenReturn("api/x");

        Response r = mapper.toResponse(new IllegalStateException("conexión a 10.0.0.5 con clave secreta"));

        assertThat(r.getStatus()).isEqualTo(500);
        ErrorResponse cuerpo = (ErrorResponse) r.getEntity();
        assertThat(cuerpo.code()).isEqualTo("ERROR_INTERNO");
        assertThat(cuerpo.status()).isEqualTo(500);
        assertThat(cuerpo.message()).doesNotContain("10.0.0.5").doesNotContain("secreta");
        assertThat(cuerpo.path()).isEqualTo("/api/x");
        assertThat(cuerpo.details()).isEmpty();
    }
}
