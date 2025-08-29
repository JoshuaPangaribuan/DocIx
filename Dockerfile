# Multi-stage build for DocIx Application

# Stage 1: Build stage
FROM gradle:8.5-jdk21 as builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# Copy source code
COPY src/ src/

# Build the application
RUN gradle build -x test --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre

LABEL maintainer="DocIx Team"
LABEL description="DocIx - Document Indexer Search Engine"
LABEL version="1.0.0"

WORKDIR /app

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r docix && useradd -r -g docix docix

# Copy jar from builder stage
COPY --from=builder /app/build/libs/DocIx-*.jar app.jar

# Change ownership
RUN chown -R docix:docix /app

# Switch to non-root user
USER docix

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
