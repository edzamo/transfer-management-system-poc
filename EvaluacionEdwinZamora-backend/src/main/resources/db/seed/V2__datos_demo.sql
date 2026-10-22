-- Datos de demostración (3 cuentas y su historial). NO se aplican por defecto:
-- solo se cargan cuando QUARKUS_FLYWAY_LOCATIONS incluye classpath:db/seed (ver docker-compose.yml).
-- Las transferencias son de hace 2 días para no consumir el límite diario de "hoy".
INSERT INTO cuentas (numero, titular, tipo, saldo, estado) VALUES
    ('1710000001', 'Ana Pérez',    'AHORRO',    4200.00, 'ACTIVA'),
    ('1710000002', 'Luis Mora',    'CORRIENTE', 8750.50, 'ACTIVA'),
    ('1710000003', 'María Torres', 'AHORRO',     300.00, 'INACTIVA');

INSERT INTO transferencias (cuenta_origen, cuenta_destino, monto, estado, motivo, fecha) VALUES
    ('1710000001', '1710000002',  800.00, 'COMPLETADA',  NULL, UTC_TIMESTAMP(6) - INTERVAL 49 HOUR),
    ('1710000002', '1710000001', 5200.00, 'EN_REVISION', NULL, UTC_TIMESTAMP(6) - INTERVAL 48 HOUR),
    ('1710000001', '1710000002',  150.00, 'COMPLETADA',  NULL, UTC_TIMESTAMP(6) - INTERVAL 47 HOUR),
    ('1710000001', '1710000003',  100.00, 'RECHAZADA',   'La cuenta ******0003 está inactiva', UTC_TIMESTAMP(6) - INTERVAL 46 HOUR);
