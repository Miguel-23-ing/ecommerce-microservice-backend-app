# ========== ETAPA DE COMPILACIÓN ==========
# Utiliza Maven 3.8.4 con OpenJDK 11 en versión slim para compilar el código fuente
# Esta etapa compila el proyecto Maven y genera el JAR ejecutable
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app

# Copia el archivo pom.xml y descarga todas las dependencias previamente
# Esto permite un mejor aprovechamiento del caché de Docker
COPY pom.xml ./

RUN mvn dependency:go-offline -B

# Copia el código fuente y compila el proyecto, omitiendo tests para acelerar el build
COPY src ./src
RUN mvn clean package -DskipTests

# ========== ETAPA DE RUNTIME ==========
# Utiliza OpenJDK 11 JRE (sin compilador) para reducir el tamaño de la imagen
FROM eclipse-temurin:11-jre-jammy

# Instala curl para el health check HTTP
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# ========== ARGUMENTOS DE CONSTRUCCIÓN ==========
# Versión del proyecto
ARG PROJECT_VERSION=0.1.0
# Ambiente (dev, staging, prod)
ARG ENVIRONMENT=dev
# ID del usuario no-root para ejecutar la aplicación
ARG USER_ID=1001
# ID del grupo para el usuario no-root
ARG GROUP_ID=1001

# ========== VARIABLES DE ENTORNO ==========
# Perfil de Spring Boot activo (define configuración específica del ambiente)
ENV SPRING_PROFILES_ACTIVE=${ENVIRONMENT}
# Opciones JVM: memoria máxima 512MB, inicial 256MB, G1GC para mejor rendimiento en contenedores
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
# Puerto donde escucha el servicio de envíos
ENV SERVER_PORT=8600

# ========== SEGURIDAD: USUARIO NO-ROOT ==========
# Crea un grupo y usuario no-root para ejecutar la aplicación (mejora de seguridad)
RUN groupadd -g ${GROUP_ID} appuser && \
    useradd -r -u ${USER_ID} -g appuser appuser

# Crea directorio home y asigna permisos al usuario
RUN mkdir -p /home/app && chown -R appuser:appuser /home/app

# ========== CONFIGURACIÓN DEL DIRECTORIO DE TRABAJO ==========
WORKDIR /home/app
# Cambia al usuario no-root para ejecutar la aplicación
USER appuser

# ========== COPIA DEL JAR COMPILADO ==========
# Copia el JAR generado desde la etapa de compilación (desde el contenedor anterior)
# --chown asegura que el archivo pertenece al usuario appuser
COPY --chown=appuser:appuser target/shipping-service-v${PROJECT_VERSION}.jar shipping-service.jar

# ========== EXPOSICIÓN DE PUERTOS ==========
# Expone el puerto 8600 donde escucha el servicio de envíos
EXPOSE ${SERVER_PORT}

# ========== HEALTH CHECK ==========
# Verifica cada 30s si el servicio está saludable
# Timeout de 10s, espera 60s antes de comenzar checks
# Falla después de 3 intentos fallidos
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# ========== PUNTO DE ENTRADA ==========
# Ejecuta la aplicación Java con los parámetros configurados
# Hereda JAVA_OPTS, perfiles de Spring y puerto desde variables de entorno
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$SERVER_PORT -Dmanagement.server.port=$SERVER_PORT -jar shipping-service.jar"]
