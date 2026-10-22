# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage: resolve dependencies first so the layer survives source-only
# changes, then package the application.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package \
    && mv target/*.jar /build/app.jar

# ---------------------------------------------------------------------------
# Runtime stage: JRE only, non-root user, no build tooling.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appgroup \
    && useradd --system --gid appgroup --home-dir /app --shell /usr/sbin/nologin appuser

WORKDIR /app
COPY --from=build --chown=appuser:appgroup /build/app.jar /app/app.jar
RUN mkdir -p /app/data && chown appuser:appgroup /app/data

USER appuser
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=docker

# Liveness probe: the public root endpoint reports that the application is up.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/ || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
