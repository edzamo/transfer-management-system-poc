package com.netby.transferencias.adapter.in.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.netby.transferencias.domain.error.CuentaBloqueadaException;
import com.netby.transferencias.domain.error.CuentaDuplicadaException;
import com.netby.transferencias.domain.error.CuentaNoEncontradaException;
import com.netby.transferencias.domain.error.DatosInvalidosException;
import com.netby.transferencias.domain.error.DominioException;
import com.netby.transferencias.domain.error.LimiteDiarioExcedidoException;
import com.netby.transferencias.domain.error.ReglaNegocioException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Único punto de traducción de errores a HTTP (formato D5). Nunca filtra stacktraces. */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);
    private static final String MENSAJE_INTERNO = "Ocurrió un error inesperado. Intente nuevamente más tarde.";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable error) {
        ErrorResponse cuerpo = switch (error) {
            case DominioException e -> deDominio(e);
            case ConstraintViolationException e -> deValidacion(e);
            case JsonProcessingException e -> validacion("El cuerpo de la solicitud no es un JSON válido o contiene valores no permitidos");
            case WebApplicationException e -> deHttp(e);
            default -> interno(error);
        };
        return Response.status(cuerpo.status()).type(MediaType.APPLICATION_JSON).entity(cuerpo).build();
    }

    /*
     * Quarkus registra mappers propios para estas dos excepciones, más específicos que Throwable, así que ganarían
     * al mapper genérico. Se redeclaran aquí (misma clase, mismo formato) para que el contrato de error sea único.
     */
    @ServerExceptionMapper
    public Response violaciones(ConstraintViolationException e) {
        return toResponse(e);
    }

    @ServerExceptionMapper
    public Response jsonInvalido(MismatchedInputException e) {
        return toResponse(e);
    }

    private ErrorResponse deDominio(DominioException e) {
        int estado = switch (e) {
            case DatosInvalidosException x -> 400;
            case CuentaNoEncontradaException x -> 404;
            case CuentaDuplicadaException x -> 409;
            case CuentaBloqueadaException x -> 409;
            case ReglaNegocioException x -> 422;
        };
        List<ErrorResponse.Detalle> detalles = e instanceof DatosInvalidosException d && d.campo() != null
                ? List.of(new ErrorResponse.Detalle(d.campo(), d.getMessage()))
                : List.of();
        BigDecimal restante = e instanceof LimiteDiarioExcedidoException l ? l.montoRestante() : null;
        LOG.warnf("Error de negocio code=%s status=%d path=%s", e.code(), estado, ruta());
        return new ErrorResponse(Instant.now(), estado, e.code(), e.getMessage(), ruta(), detalles, restante);
    }

    private ErrorResponse deValidacion(ConstraintViolationException e) {
        List<ErrorResponse.Detalle> detalles = e.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.Detalle(campo(v), v.getMessage()))
                .distinct()
                .sorted(Comparator.comparing(ErrorResponse.Detalle::field))
                .toList();
        return new ErrorResponse(Instant.now(), 400, DatosInvalidosException.CODE, "La solicitud contiene datos inválidos",
                ruta(), detalles, null);
    }

    private static String campo(ConstraintViolation<?> violacion) {
        String nombre = "";
        for (Path.Node nodo : violacion.getPropertyPath()) {
            nombre = nodo.getName();
        }
        return nombre;
    }

    private ErrorResponse validacion(String mensaje) {
        return new ErrorResponse(Instant.now(), 400, DatosInvalidosException.CODE, mensaje, ruta(), List.of(), null);
    }

    private ErrorResponse deHttp(WebApplicationException e) {
        int estado = e.getResponse().getStatus();
        if (estado >= 500) {
            return interno(e);
        }
        if (estado == 404 && e.getCause() != null) {
            // JAX-RS responde 404 cuando falla la conversión de un parámetro (número, fecha, enum): es un 400.
            return validacion("Uno de los parámetros de la solicitud tiene un formato inválido");
        }
        String codigo = switch (estado) {
            case 400 -> DatosInvalidosException.CODE;
            case 404 -> "RECURSO_NO_ENCONTRADO";
            case 405 -> "METODO_NO_PERMITIDO";
            case 415 -> "MEDIA_TYPE_NO_SOPORTADO";
            default -> "ERROR_HTTP";
        };
        return new ErrorResponse(Instant.now(), estado, codigo, mensajeHttp(estado), ruta(), List.of(), null);
    }

    private static String mensajeHttp(int estado) {
        return switch (estado) {
            case 400 -> "Solicitud inválida";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 415 -> "Tipo de contenido no soportado";
            default -> "No se pudo procesar la solicitud";
        };
    }

    private ErrorResponse interno(Throwable error) {
        LOG.error("Error no controlado en " + ruta(), error);
        return new ErrorResponse(Instant.now(), 500, "ERROR_INTERNO", MENSAJE_INTERNO, ruta(), List.of(), null);
    }

    private String ruta() {
        String path = uriInfo.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }
}
