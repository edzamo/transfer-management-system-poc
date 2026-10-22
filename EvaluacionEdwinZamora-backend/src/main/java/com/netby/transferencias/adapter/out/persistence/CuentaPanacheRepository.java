package com.netby.transferencias.adapter.out.persistence;

import com.netby.transferencias.adapter.out.persistence.entity.CuentaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CuentaPanacheRepository implements PanacheRepository<CuentaEntity> {
}
