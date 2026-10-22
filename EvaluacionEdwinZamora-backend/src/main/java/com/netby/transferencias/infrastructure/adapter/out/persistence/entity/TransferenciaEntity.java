package com.netby.transferencias.infrastructure.adapter.out.persistence.entity;

import com.netby.transferencias.domain.transferencia.EstadoTransferencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transferencias")
public class TransferenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "cuenta_origen")
    public String cuentaOrigen;

    @Column(name = "cuenta_destino")
    public String cuentaDestino;

    @Column(name = "monto")
    public BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    public EstadoTransferencia estado;

    @Column(name = "motivo")
    public String motivo;

    @Column(name = "fecha")
    public Instant fecha;
}
