package com.netby.transferencias.infrastructure.adapter.out.persistence.entity;

import com.netby.transferencias.domain.cuenta.EstadoCuenta;
import com.netby.transferencias.domain.cuenta.TipoCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Modelo de persistencia: sin reglas; el esquema y el dominio garantizan la integridad. */
@Entity
@Table(name = "cuentas")
public class CuentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "numero")
    public String numero;

    @Column(name = "titular")
    public String titular;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    public TipoCuenta tipo;

    @Column(name = "saldo")
    public BigDecimal saldo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    public EstadoCuenta estado;
}
