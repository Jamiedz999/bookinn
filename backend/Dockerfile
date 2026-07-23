# syntax=docker/dockerfile:1
#
# Multi-stage build for the Spring Boot backend.
#   Stage 1 (build)   — full JDK + Maven wrapper, compiles the fat jar (tests skipped; CI runs them).
#   Stage 2 (runtime) — slim JRE only, no build tooling, runs as a non-root user.
# Built for linux/arm64 to run on the Graviton (t4g) EC2 host — see ADR 0005.

# ---- build stage ----
# Use the official multi-arch Maven image rather than the ./mvnw wrapper: the wrapper downloads and
# untars a Maven distribution at build time, which fails under QEMU arm64 emulation ("tar: Cannot
# open: Function not implemented"). A preinstalled Maven keeps the cross-build (and QEMU-based CI)
# reliable. Maven 3.9.x matches the wrapper's pinned line.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Then compile. Tests need Testcontainers (Docker-in-Docker) and run in CI, not in the image build.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used by the compose healthcheck to hit /actuator/health.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as an unprivileged user, never root.
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=build /build/target/backend-*.jar app.jar
USER app

EXPOSE 8080
# Cap heap relative to the container's memory (t4g.small = 2 GB) so the JVM stays inside its limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
# exec so the JVM becomes PID 1 and receives SIGTERM directly (graceful shutdown, no 10s kill wait).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
