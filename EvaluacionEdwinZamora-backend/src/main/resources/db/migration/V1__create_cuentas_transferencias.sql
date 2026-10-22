CREATE TABLE cuentas (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    numero     CHAR(10)       NOT NULL,
    titular    VARCHAR(150)   NOT NULL,
    tipo       VARCHAR(10)    NOT NULL,
    saldo      DECIMAL(19, 2) NOT NULL,
    estado     VARCHAR(10)    NOT NULL,
    created_at DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cuentas_numero UNIQUE (numero),
    CONSTRAINT ck_cuentas_saldo CHECK (saldo >= 0),
    CONSTRAINT ck_cuentas_tipo CHECK (tipo IN ('AHORRO', 'CORRIENTE')),
    CONSTRAINT ck_cuentas_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
) ENGINE = InnoDB;

CREATE TABLE transferencias (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    cuenta_origen  VARCHAR(10)    NOT NULL,
    cuenta_destino VARCHAR(10)    NOT NULL,
    monto          DECIMAL(19, 2) NOT NULL,
    estado         VARCHAR(15)    NOT NULL,
    motivo         VARCHAR(255)   NULL,
    fecha          DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_transferencias_monto CHECK (monto > 0),
    CONSTRAINT ck_transferencias_estado CHECK (estado IN ('COMPLETADA', 'RECHAZADA', 'EN_REVISION')),
    INDEX idx_transferencias_origen_fecha (cuenta_origen, fecha),
    INDEX idx_transferencias_destino_fecha (cuenta_destino, fecha),
    INDEX idx_transferencias_fecha (fecha)
) ENGINE = InnoDB;
