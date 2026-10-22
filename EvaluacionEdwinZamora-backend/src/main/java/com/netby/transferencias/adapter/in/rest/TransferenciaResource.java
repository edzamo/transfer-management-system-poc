package com.netby.transferencias.adapter.in.rest;

import com.netby.transferencias.adapter.in.rest.dto.PaginaResponse;
import com.netby.transferencias.adapter.in.rest.dto.TransferenciaRequest;
import com.netby.transferencias.adapter.in.rest.dto.TransferenciaResponse;
import com.netby.transferencias.adapter.in.rest.mapper.RestMapper;
import com.netby.transferencias.application.in.ConsultarHistorialTransferencias;
import com.netby.transferencias.application.in.RealizarTransferencia;
import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import com.netby.transferencias.domain.transferencia.Transferencia;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
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

@Path("/api/transferencias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transferencias", description = "Ejecución e historial de transferencias entre cuentas")
public class TransferenciaResource {

    private static final Logger LOG = Logger.getLogger(TransferenciaResource.class);
    private static final String JSON = MediaType.APPLICATION_JSON;

    private final RealizarTransferencia realizarTransferencia;
    private final ConsultarHistorialTransferencias consultarHistorial;
    private final RestMapper mapper;

    @Inject
    public TransferenciaResource(RealizarTransferencia realizarTransferencia,
                                 ConsultarHistorialTransferencias consultarHistorial, RestMapper mapper) {
        this.realizarTransferencia = realizarTransferencia;
        this.consultarHistorial = consultarHistorial;
        this.mapper = mapper;
    }

    @POST
    @Operation(summary = "Realizar una transferencia",
            description = "Mueve el monto entre dos cuentas de forma atómica. Si es sospechosa (monto > 5000 o más de "
                    + "5 transferencias del origen en la última hora) se registra EN_REVISION y los saldos igualmente "
                    + "se actualizan. Los rechazos por regla (422) quedan registrados como RECHAZADA.")
    @RequestBody(content = @Content(mediaType = JSON, schema = @Schema(implementation = TransferenciaRequest.class),
            examples = @ExampleObject(name = "Transferencia", value = """
                    {"cuentaOrigen":"1234567890","cuentaDestino":"0987654321","monto":250.75}""")))
    @APIResponse(responseCode = "201", description = "Transferencia registrada (COMPLETADA o EN_REVISION)",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = TransferenciaResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"id":10,"cuentaOrigen":"1234567890","cuentaDestino":"0987654321","monto":250.75,"estado":"COMPLETADA","motivo":null,"fecha":"2026-10-22T15:00:00Z"}""")))
    @APIResponse(responseCode = "400", description = "Datos inválidos (monto <= 0, más de 2 decimales, origen = destino)",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"timestamp":"2026-10-22T15:00:00Z","status":400,"code":"VALIDACION","message":"El monto debe ser mayor a cero","path":"/api/transferencias","details":[{"field":"monto","message":"El monto debe ser mayor a cero"}]}""")))
    @APIResponse(responseCode = "404", description = "La cuenta origen o destino no existe",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"timestamp":"2026-10-22T15:00:00Z","status":404,"code":"CUENTA_NO_ENCONTRADA","message":"No existe la cuenta ******4321","path":"/api/transferencias","details":[]}""")))
    @APIResponse(responseCode = "409", description = "La cuenta está siendo modificada por otra operación; reintentar",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"timestamp":"2026-10-22T15:00:00Z","status":409,"code":"CUENTA_BLOQUEADA","message":"La cuenta ******7890 está siendo modificada por otra operación; reintente","path":"/api/transferencias","details":[]}""")))
    @APIResponse(responseCode = "422",
            description = "Regla de negocio: CUENTA_INACTIVA, SALDO_INSUFICIENTE o LIMITE_DIARIO_EXCEDIDO (con montoRestante)",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                            {"timestamp":"2026-10-22T15:00:00Z","status":422,"code":"LIMITE_DIARIO_EXCEDIDO","message":"Límite diario excedido. Monto restante disponible: 500.00","path":"/api/transferencias","details":[],"montoRestante":500.00}""")))
    @APIResponse(responseCode = "500", description = "Error interno",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public Response transferir(@Valid TransferenciaRequest request) {
        Transferencia t = realizarTransferencia.realizar(mapper.toCommand(request));
        LOG.infof("Transferencia id=%d estado=%s", t.id(), t.estado());
        return Response.status(Response.Status.CREATED).entity(mapper.toTransferenciaResponse(t)).build();
    }

    @GET
    @Operation(summary = "Historial de transferencias",
            description = "Consulta paginada con filtros opcionales, ordenada por fecha descendente.")
    @Parameter(name = "origen", in = ParameterIn.QUERY, description = "Número de cuenta origen", example = "1234567890")
    @Parameter(name = "destino", in = ParameterIn.QUERY, description = "Número de cuenta destino", example = "0987654321")
    @Parameter(name = "desde", in = ParameterIn.QUERY, description = "Fecha inicial inclusive (ISO-8601, UTC)",
            example = "2026-10-01T00:00:00Z")
    @Parameter(name = "hasta", in = ParameterIn.QUERY, description = "Fecha final inclusive (ISO-8601, UTC)",
            example = "2026-10-31T23:59:59Z")
    @Parameter(name = "estado", in = ParameterIn.QUERY, description = "COMPLETADA, RECHAZADA o EN_REVISION",
            example = "COMPLETADA")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Página (base 0)", example = "0")
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Tamaño de página (1 a 200)", example = "20")
    @APIResponse(responseCode = "200", description = "Página de transferencias",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = PaginaResponse.class),
                    examples = @ExampleObject(name = "Ejemplo", value = """
                    {"content":[{"id":10,"cuentaOrigen":"1234567890","cuentaDestino":"0987654321","monto":250.75,"estado":"COMPLETADA","motivo":null,"fecha":"2026-10-22T15:00:00Z"}],"page":0,"size":20,"totalElements":1,"totalPages":1}""")))
    @APIResponse(responseCode = "400", description = "Filtros o paginación inválidos",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Error interno",
            content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class)))
    public PaginaResponse<TransferenciaResponse> historial(
            @QueryParam("origen") String origen,
            @QueryParam("destino") String destino,
            @QueryParam("desde") Instant desde,
            @QueryParam("hasta") Instant hasta,
            @QueryParam("estado") EstadoTransferencia estado,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return mapper.toTransferenciasPagina(consultarHistorial.consultar(
                new ConsultarHistorialTransferencias.Query(origen, destino, desde, hasta, estado, page, size)));
    }
}
