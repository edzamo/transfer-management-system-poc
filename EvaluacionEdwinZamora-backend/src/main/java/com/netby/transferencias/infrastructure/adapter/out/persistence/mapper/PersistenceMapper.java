package com.netby.transferencias.infrastructure.adapter.out.persistence.mapper;

import com.netby.transferencias.infrastructure.adapter.out.persistence.entity.CuentaEntity;
import com.netby.transferencias.infrastructure.adapter.out.persistence.entity.TransferenciaEntity;
import com.netby.transferencias.domain.cuenta.Cuenta;
import com.netby.transferencias.domain.cuenta.NumeroCuenta;
import com.netby.transferencias.domain.cuenta.Titular;
import com.netby.transferencias.domain.dinero.Dinero;
import com.netby.transferencias.domain.transferencia.Transferencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Dominio <-> persistencia. Al leer se rehidratan los VO, de modo que datos corruptos fallan al leer. */
@Mapper
public interface PersistenceMapper {

    @Mapping(target = "id", expression = "java(cuenta.id())")
    @Mapping(target = "numero", expression = "java(cuenta.numero().valor())")
    @Mapping(target = "titular", expression = "java(cuenta.titular().valor())")
    @Mapping(target = "tipo", expression = "java(cuenta.tipo())")
    @Mapping(target = "saldo", expression = "java(cuenta.saldo().valor())")
    @Mapping(target = "estado", expression = "java(cuenta.estado())")
    CuentaEntity toEntity(Cuenta cuenta);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "titular", expression = "java(cuenta.titular().valor())")
    @Mapping(target = "tipo", expression = "java(cuenta.tipo())")
    @Mapping(target = "saldo", expression = "java(cuenta.saldo().valor())")
    @Mapping(target = "estado", expression = "java(cuenta.estado())")
    void actualizar(Cuenta cuenta, @MappingTarget CuentaEntity entity);

    default Cuenta toDomain(CuentaEntity e) {
        return Cuenta.reconstruir(e.id, NumeroCuenta.de(e.numero), Titular.de(e.titular), e.tipo,
                Dinero.de(e.saldo), e.estado);
    }

    @Mapping(target = "id", expression = "java(t.id())")
    @Mapping(target = "cuentaOrigen", expression = "java(t.cuentaOrigen().valor())")
    @Mapping(target = "cuentaDestino", expression = "java(t.cuentaDestino().valor())")
    @Mapping(target = "monto", expression = "java(t.monto().valor())")
    @Mapping(target = "estado", expression = "java(t.estado())")
    @Mapping(target = "motivo", expression = "java(t.motivo())")
    @Mapping(target = "fecha", expression = "java(t.fecha())")
    TransferenciaEntity toEntity(Transferencia t);

    default Transferencia toDomain(TransferenciaEntity e) {
        return Transferencia.reconstruir(e.id, NumeroCuenta.de(e.cuentaOrigen), NumeroCuenta.de(e.cuentaDestino),
                Dinero.de(e.monto), e.estado, e.motivo, e.fecha);
    }
}
