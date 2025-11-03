
FROM openjdk:11
ARG PROJECT_VERSION=0.1.0
RUN mkdir -p /home/app
# Multi-stage build for proxy-client
FROM maven:3.9.6 AS build

# Copy the entire project into the build container
COPY .. /workspace

# Build only this module
WORKDIR /workspace/proxy-client
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre

WORKDIR /app

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Copy the built jar from build stage
COPY --from=build /workspace/proxy-client/target/proxy-client-v0.1.0.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8900

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8900/app/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]


# Run the application

ENTRYPOINT ["java", "-jar", "app.jar"]
