# Base de datos del proyecto: MySQL 8.0 con configuración propia.
# El esquema NO se crea aquí: lo aplica Flyway al arrancar el backend
# (EvaluacionEdwinZamora-backend/src/main/resources/db/migration).
FROM mysql:8.0

COPY conf.d/custom.cnf /etc/mysql/conf.d/custom.cnf

EXPOSE 3306
HEALTHCHECK --interval=10s --timeout=5s --retries=10 --start-period=20s \
  CMD mysqladmin ping -h 127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --silent || exit 1
