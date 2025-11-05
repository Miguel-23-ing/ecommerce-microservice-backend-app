# Etapa 1: Construcción (Build stage)
# Usar imagen Maven 3.8.4 con OpenJDK 11 para compilar la aplicación
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app

# Copiar archivo de configuración de dependencias
COPY pom.xml ./pom.xml

# Descargar todas las dependencias necesarias con anticipación (optimiza capas de Docker)
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar la aplicación (genera .jar en /app/target)
COPY src ./src
RUN mvn clean package -DskipTests


# Etapa 2: Ejecución (Runtime stage)
# Usar imagen OpenJDK 11 JRE (más pequeña, solo para ejecutar, no compilar)
FROM eclipse-temurin:11-jre-jammy

# Instalar curl para el health check (verifica que el servidor esté listo)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*


# Argumentos de construcción (personalizables durante docker build)
# PROJECT_VERSION: versión del artefacto JAR a copiar
# ENVIRONMENT: perfil Spring activo (dev, prod, test, etc.)
# USER_ID y GROUP_ID: UIDs para el usuario no-root que ejecutará la aplicación
ARG PROJECT_VERSION=0.1.0
ARG ENVIRONMENT=dev
ARG USER_ID=1001
ARG GROUP_ID=1001

# Variables de entorno configurables durante docker run
# SPRING_PROFILES_ACTIVE: activa el perfil correspondiente en Spring Boot
# JAVA_OPTS: parámetros JVM (heap máximo/mínimo, garbage collector, soporte contenedor)
# SERVER_PORT: puerto en el que escucha la aplicación (API Gateway por defecto en 8080)
ENV SPRING_PROFILES_ACTIVE=${ENVIRONMENT}
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SERVER_PORT=8080


# Crear grupo de usuario y usuario no-root (mejora seguridad: la app no corre como root)
RUN groupadd -g ${GROUP_ID} appuser && \
    useradd -r -u ${USER_ID} -g appuser appuser

# Crear directorio de inicio y asignar permisos al usuario no-root
RUN mkdir -p /home/app && \
    chown -R appuser:appuser /home/app

# Definir directorio de trabajo y cambiar al usuario no-root
WORKDIR /home/app
USER appuser


# Copiar archivo JAR compilado desde la etapa build (reduce tamaño final eliminando dependencias de compilación)
# --chown: asegura que el archivo pertenezca al usuario appuser (no root)
COPY --chown=appuser:appuser target/api-gateway-v${PROJECT_VERSION}.jar api-gateway.jar

# Exponer puerto en el contenedor (documentación; Docker debe usar -p para mapear)
EXPOSE ${SERVER_PORT}

# Health check: verifica cada 30s si la aplicación responde en /actuator/health
# Si falla 3 veces consecutivas, marca el contenedor como unhealthy
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Comando de inicio: ejecuta API Gateway con parámetros de JVM y configuración de Spring
# Enruta solicitudes HTTP a microservicios registrados en Eureka
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$SERVER_PORT -Dmanagement.server.port=$SERVER_PORT -jar api-gateway.jar"]
