# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

COPY src ./src
RUN ./mvnw clean package -DskipTests -B -q

FROM eclipse-temurin:21-jre
WORKDIR /app

# curl para el healthcheck de docker-compose (la imagen base no lo trae).
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario no-root: los directorios se crean y se les asigna owner ACA, antes
# de USER app, para que los volúmenes montados por Docker (adjuntos_data,
# backend_logs) hereden el ownership correcto en el primer mount.
RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /app/logs /data/adjuntos \
    && chown -R app:app /app /data/adjuntos

COPY --from=build /app/target/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080
# Forma shell (no exec-array) para que $JAVA_OPTS se expanda. JDK 21 ya
# detecta el límite de memoria del cgroup solo, pero MaxRAMPercentage hay que
# fijarlo explícito o cae al default de 25% del límite del contenedor.
ENTRYPOINT java $JAVA_OPTS \
    -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom \
    -jar app.jar
