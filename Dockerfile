# ─── Stage 1: Build ───
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew clean bootJar --no-daemon -x test

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR
COPY --from=builder /app/build/libs/banking-transaction-system.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM tuning for container environments
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
