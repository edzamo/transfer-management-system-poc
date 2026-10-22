# Etapa 1: compilación (sin tests; los tests corren en CI / localmente)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: runtime mínimo
FROM eclipse-temurin:21-jre-alpine
WORKDIR /deployments
RUN addgroup -S app && adduser -S -G app app
COPY --chown=app:app --from=builder /build/target/quarkus-app/ ./
USER app
EXPOSE 8080
ENV JAVA_OPTS_APPEND="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]
