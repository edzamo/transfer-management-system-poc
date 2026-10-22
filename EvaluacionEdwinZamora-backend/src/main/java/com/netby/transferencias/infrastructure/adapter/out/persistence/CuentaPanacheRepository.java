package com.netby.transferencias.infrastructure.adapter.out.persistence;

import com.netby.transferencias.infrastructure.adapter.out.persistence.entity.CuentaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CuentaPanacheRepository implements PanacheRepository<CuentaEntity> {
}
