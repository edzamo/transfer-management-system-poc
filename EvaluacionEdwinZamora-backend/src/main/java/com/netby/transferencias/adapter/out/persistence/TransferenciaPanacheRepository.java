package com.netby.transferencias.adapter.out.persistence;

import com.netby.transferencias.adapter.out.persistence.entity.TransferenciaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransferenciaPanacheRepository implements PanacheRepository<TransferenciaEntity> {
}
