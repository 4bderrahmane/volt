FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# wget is needed by the compose healthcheck and is absent from the alpine JRE.
RUN apk add --no-cache wget && addgroup -S volt && adduser -S volt -G volt

# Ordered least- to most-frequently-changed, matching the layer extraction.
COPY --from=build --chown=volt:volt /build/extracted/dependencies/ ./
COPY --from=build --chown=volt:volt /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=volt:volt /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=volt:volt /build/extracted/application/ ./

USER volt
EXPOSE 8081

# MaxRAMPercentage rather than a fixed -Xmx: the JVM reads the container's
# memory limit, so the heap adapts if compose limits change. A fixed heap in a
# container is how a service gets OOM-killed by the kernel rather than throwing
# a diagnosable OutOfMemoryError.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
