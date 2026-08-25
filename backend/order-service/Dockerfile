# =============================================================================
# order-service — multi-stage build (technical specification §8)
# Identical to catalog-service's; see that file for the rationale on each stage.
# =============================================================================

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache wget && \
    addgroup -S volt && adduser -S volt -G volt

COPY --from=build --chown=volt:volt /build/extracted/dependencies/ ./
COPY --from=build --chown=volt:volt /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=volt:volt /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=volt:volt /build/extracted/application/ ./

USER volt
EXPOSE 8082

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# The executable jar was extracted into layers above; run the exploded layout.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
