package com.netby.transferencias.adapter.in.rest;

import com.netby.transferencias.adapter.in.rest.dto.CuentaRequest;
import com.netby.transferencias.adapter.in.rest.dto.CuentaResponse;
import com.netby.transferencias.adapter.in.rest.dto.PaginaResponse;
import com.netby.transferencias.adapter.in.rest.dto.ResumenCuentaResponse;
import com.netby.transferencias.adapter.in.rest.mapper.RestMapper;
import com.netby.transferencias.application.in.ListarCuentas;
import com.netby.transferencias.application.in.ObtenerResumenCuenta;
import com.netby.transferencias.application.in.RegistrarCuenta;
import com.netby.transferencias.domain.cuenta.Cuenta;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/api/cuentas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Cuentas", description = "Registro, listado y resumen de cuentas bancarias")
public class CuentaResource {

    private static final Logger LOG = Logger.getLogger(CuentaResource.class);
    private static final String JSON = MediaType.APPLICATION_JSON;

    private final RegistrarCuenta registrarCuenta;
    private final ListarCuentas listarCuentas;
    private final ObtenerResumenCuenta obtenerResumen;
    private final RestMapper mapper;

    @Inject
    public CuentaResource(RegistrarCuenta registrarCuenta, ListarCuentas listarCuentas,
                          ObtenerResumenCuenta obtenerResumen, RestMapper mapper) {
        this.registrarCuenta = registrarCuenta;
        this.listarCuentas = listarCuentas;
        this.obtenerResumen = obtenerResumen;
        this.mapper = mapper;
    }

    @POST
    @Operation(summary = "Registrar una cuenta",
            description = "Crea una cuenta con número único de 10 dígitos, titular, tipo, saldo inicial (>= 0) y estado.")
    @RequestBody(content = @Content(mediaType = JSON, schema = @Schema(implementation = CuentaRequest.class),
            examples = @ExampleObject(name = "Cuenta de ahorro", value = """
                    {"numero":"1234567890","titular":"Ana Pérez","tipo":"AHORRO","saldoInicial":1500.00,"estado":"ACTIVA"}""")))
    @APIResponse(responseCode = "201", description = "Cuenta creada",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = CuentaResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"id":1,"numero":"1234567890","titular":"Ana Pérez","tipo":"AHORRO","saldo":1500.00,"estado":"ACTIVA"}""")))
    @APIResponse(responseCode = "400", description = "Datos inválidos (número, titular, saldo negativo, JSON)",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"timestamp":"2026-10-22T15:00:00Z","status":400,"code":"VALIDACION","message":"El campo saldoInicial no puede ser negativo","path":"/api/cuentas","details":[{"field":"saldoInicial","message":"El campo saldoInicial no puede ser negativo"}]}""")))
    @APIResponse(responseCode = "409", description = "Ya existe una cuenta con ese número",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"timestamp":"2026-10-22T15:00:00Z","status":409,"code":"CUENTA_DUPLICADA","message":"Ya existe una cuenta con el número ******7890","path":"/api/cuentas","details":[]}""")))
    @APIResponse(responseCode = "500", description = "Error interno",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response crear(@Valid CuentaRequest request) {
        Cuenta cuenta = registrarCuenta.registrar(mapper.toCommand(request));
        LOG.infof("Cuenta creada id=%d", cuenta.id());
        return Response.status(Response.Status.CREATED).entity(mapper.toCuentaResponse(cuenta)).build();
    }

    @GET
    @Operation(summary = "Listar cuentas", description = "Lista paginada de cuentas ordenadas por id.")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Página (base 0)", example = "0")
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Tamaño de página (1 a 200)", example = "20")
    @APIResponse(responseCode = "200", description = "Página de cuentas",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = PaginaResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                    {"content":[{"id":1,"numero":"1234567890","titular":"Ana Pérez","tipo":"AHORRO","saldo":1500.00,"estado":"ACTIVA"}],"page":0,"size":20,"totalElements":1,"totalPages":1}""")))
    @APIResponse(responseCode = "400", description = "Paginación inválida",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Error interno",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public PaginaResponse<CuentaResponse> listar(@QueryParam("page") @DefaultValue("0") int page,
                                                 @QueryParam("size") @DefaultValue("20") int size) {
        return mapper.toCuentasPagina(listarCuentas.listar(new ListarCuentas.Query(page, size)));
    }

    @GET
    @Path("/{id}/resumen")
    @Operation(summary = "Resumen de una cuenta",
            description = "Saldo actual y, para el día calendario en curso, total transferido, cantidad de "
                    + "transferencias (COMPLETADA + EN_REVISION) y cantidad de sospechosas (EN_REVISION).")
    @Parameter(name = "id", in = ParameterIn.PATH, description = "Identificador de la cuenta", example = "1")
    @APIResponse(responseCode = "200", description = "Resumen de la cuenta",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ResumenCuentaResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"cuentaId":1,"saldoActual":1500.00,"totalTransferidoHoy":600.00,"cantidadTransferenciasHoy":3,"cantidadSospechosasHoy":1}""")))
    @APIResponse(responseCode = "400", description = "Identificador inválido",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "La cuenta no existe",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"timestamp":"2026-10-22T15:00:00Z","status":404,"code":"CUENTA_NO_ENCONTRADA","message":"No existe la cuenta con id 99","path":"/api/cuentas/99/resumen","details":[]}""")))
    @APIResponse(responseCode = "500", description = "Error interno",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public ResumenCuentaResponse resumen(@PathParam("id") Long id) {
        return mapper.toResumenResponse(obtenerResumen.obtener(new ObtenerResumenCuenta.Query(id)));
    }
}
