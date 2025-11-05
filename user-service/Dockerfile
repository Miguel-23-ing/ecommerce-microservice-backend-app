# ========== ETAPA DE RUNTIME ==========
# Utiliza OpenJDK 11 JRE (sin compilador) para reducir el tamaño de la imagen
# NOTA: El JAR debe compilarse previamente con Maven desde la raíz del proyecto
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
# Puerto donde escucha el servicio de usuarios
ENV SERVER_PORT=8700

# ========== SEGURIDAD: USUARIO NO-ROOT ==========
# Crea un grupo y usuario no-root para ejecutar la aplicación (mejora de seguridad)
RUN groupadd -g ${GROUP_ID} appuser && \
    useradd -r -u ${USER_ID} -g appuser appuser

# Crea directorio home y asigna permisos al usuario
RUN mkdir -p /home/app && \
    chown -R appuser:appuser /home/app

# ========== CONFIGURACIÓN DEL DIRECTORIO DE TRABAJO ==========
WORKDIR /home/app
# Cambia al usuario no-root para ejecutar la aplicación
USER appuser

# ========== COPIA DEL JAR COMPILADO ==========
# Copia el JAR ya compilado previamente con Maven desde la raíz del proyecto
# --chown asegura que el archivo pertenece al usuario appuser
COPY --chown=appuser:appuser target/user-service-v${PROJECT_VERSION}.jar user-service.jar

# ========== EXPOSICIÓN DE PUERTOS ==========
# Expone el puerto 8700 donde escucha el servicio de usuarios
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
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$SERVER_PORT -Dmanagement.server.port=$SERVER_PORT -jar user-service.jar"]
