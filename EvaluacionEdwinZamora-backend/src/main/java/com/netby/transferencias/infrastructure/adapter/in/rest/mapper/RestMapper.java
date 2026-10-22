package com.netby.transferencias.infrastructure.adapter.in.rest.mapper;

import com.netby.transferencias.infrastructure.adapter.in.rest.dto.CuentaRequest;
import com.netby.transferencias.infrastructure.adapter.in.rest.dto.CuentaResponse;
import com.netby.transferencias.infrastructure.adapter.in.rest.dto.PaginaResponse;
import com.netby.transferencias.infrastructure.adapter.in.rest.dto.ResumenCuentaResponse;
import com.netby.transferencias.infrastructure.adapter.in.rest.dto.TransferenciaRequest;
import com.netby.transferencias.infrastructure.adapter.in.rest.dto.TransferenciaResponse;
import com.netby.transferencias.application.dto.Pagina;
import com.netby.transferencias.application.dto.ResumenCuenta;
import com.netby.transferencias.application.in.RealizarTransferencia;
import com.netby.transferencias.application.in.RegistrarCuenta;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.transferencia.Transferencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface RestMapper {

    RegistrarCuenta.Command toCommand(CuentaRequest request);

    RealizarTransferencia.Command toCommand(TransferenciaRequest request);

    @Mapping(target = "id", expression = "java(cuenta.id())")
    @Mapping(target = "numero", expression = "java(cuenta.numero().valor())")
    @Mapping(target = "tipo", expression = "java(cuenta.tipo())")
    @Mapping(target = "estado", expression = "java(cuenta.estado())")
    @Mapping(target = "titular", expression = "java(cuenta.titular().valor())")
    @Mapping(target = "saldo", expression = "java(cuenta.saldo().valor())")
    CuentaResponse toCuentaResponse(Cuenta cuenta);

    @Mapping(target = "id", expression = "java(t.id())")
    @Mapping(target = "estado", expression = "java(t.estado())")
    @Mapping(target = "motivo", expression = "java(t.motivo())")
    @Mapping(target = "fecha", expression = "java(t.fecha())")
    @Mapping(target = "cuentaOrigen", expression = "java(t.cuentaOrigen().valor())")
    @Mapping(target = "cuentaDestino", expression = "java(t.cuentaDestino().valor())")
    @Mapping(target = "monto", expression = "java(t.monto().valor())")
    TransferenciaResponse toTransferenciaResponse(Transferencia t);

    @Mapping(target = "saldoActual", expression = "java(resumen.saldoActual().valor())")
    @Mapping(target = "totalTransferidoHoy", expression = "java(resumen.totalTransferidoHoy().valor())")
    ResumenCuentaResponse toResumenResponse(ResumenCuenta resumen);

    default PaginaResponse<CuentaResponse> toCuentasPagina(Pagina<Cuenta> pagina) {
        Pagina<CuentaResponse> mapeada = pagina.mapear(this::toCuentaResponse);
        return new PaginaResponse<>(mapeada.contenido(), mapeada.page(), mapeada.size(), mapeada.totalElements(),
                mapeada.totalPages());
    }

    default PaginaResponse<TransferenciaResponse> toTransferenciasPagina(Pagina<Transferencia> pagina) {
        Pagina<TransferenciaResponse> mapeada = pagina.mapear(this::toTransferenciaResponse);
        return new PaginaResponse<>(mapeada.contenido(), mapeada.page(), mapeada.size(), mapeada.totalElements(),
                mapeada.totalPages());
    }
}
