# Multi-stage build for user-service
FROM maven:3.9.6 AS build

# Copy the entire project
COPY .. /workspace

# Build only this module
WORKDIR /workspace/user-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre

# Set working directory
WORKDIR /app

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Copy the built jar from build stage
COPY --from=build /workspace/user-service/target/user-service-v0.1.0.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8700

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8700/user-service/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]