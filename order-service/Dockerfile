# ========== ETAPA 1: BUILD CON MAVEN ==========
# Utiliza Maven 3.8.4 con OpenJDK 11 (slim) para compilar el proyecto
# La imagen slim reduce tamaño omitiendo herramientas innecesarias
FROM maven:3.8.4-openjdk-11-slim AS build

WORKDIR /app

# Copia el archivo de configuración de dependencias
COPY pom.xml ./

# Descarga todas las dependencias del proyecto
# El flag -B activa modo batch para mejor rendimiento en CI/CD
# Esto permite reutilizar la capa de dependencias en builds posteriores
RUN mvn dependency:go-offline -B

# Copia el código fuente
COPY src ./src

# Compila el proyecto y genera el JAR ejecutable
# -DskipTests omite tests para acelerar build (opción configurable)
RUN mvn clean package -DskipTests

# ========== ETAPA 2: RUNTIME CON OPENJDK JRE ==========
# Usa solo JRE (no JDK) para reducir tamaño de imagen final (~60% menos que JDK)
# La imagen slim omite herramientas innecesarias
FROM openjdk:11-jre-slim

# Instala curl para health checks HTTP
# apt-get update y rm -rf limpian cachés para reducir tamaño de imagen
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# ========== ARGUMENTOS DE BUILD (buildtime) ==========
# Estos valores pueden sobrescribirse en tiempo de build: docker build --build-arg PROJECT_VERSION=1.0
ARG PROJECT_VERSION=0.1.0
ARG ENVIRONMENT=dev
ARG USER_ID=1001
ARG GROUP_ID=1001

# ========== VARIABLES DE ENTORNO (runtime) ==========
# Perfil activo de Spring: dev, test, prod
ENV SPRING_PROFILES_ACTIVE=${ENVIRONMENT}

# Opciones JVM:
# - Xmx512m: Memoria máxima 512MB (ajustar según disponibilidad)
# - Xms256m: Memoria inicial 256MB (mejora performance inicialmente)
# - XX:+UseG1GC: Usa garbage collector G1 (mejor para contenedores)
# - XX:+UseContainerSupport: Detecta límites de CPU/memoria del contenedor
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Puerto en el que escucha el servicio de órdenes (8300)
# Otros servicios: product=8500, favourite=8800, user=8200, payment=8400, shipping=8600
ENV SERVER_PORT=8300

# ========== SEGURIDAD: USUARIO NO-ROOT ==========
# Crea grupo de usuarios para mejorar seguridad
# Evita ejecutar la aplicación como root
RUN groupadd -g ${GROUP_ID} appuser && \
    useradd -r -u ${USER_ID} -g appuser appuser

# Prepara directorio home de la aplicación con permisos correctos
RUN mkdir -p /home/app && \
    chown -R appuser:appuser /home/app

# ========== CONFIGURACIÓN DE TRABAJO ==========
WORKDIR /home/app

# Cambia al usuario no-root creado anteriormente
USER appuser

# ========== COPIA DEL JAR COMPILADO ==========
# Copia JAR desde etapa de build, establece propietario para usuario appuser
# El JAR fue compilado como: order-service-v0.1.0.jar
COPY --from=build --chown=appuser:appuser /app/target/order-service-v${PROJECT_VERSION}.jar order-service.jar

# ========== EXPOSICIÓN DE PUERTO ==========
# Expone puerto 8300 (configurable via ENV SERVER_PORT)
# Requiere binding explícito en docker run: -p 8300:8300
EXPOSE ${SERVER_PORT}

# ========== HEALTH CHECK ==========
# Verifica que el servicio esté saludable mediante endpoint Spring Actuator
# - interval=30s: Revisa cada 30 segundos
# - timeout=10s: Espera máximo 10s por respuesta
# - start-period=60s: No empieza checks hasta 60s después de iniciar
# - retries=3: Marca como unhealthy después de 3 fallos consecutivos
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# ========== PUNTO DE ENTRADA ==========
# Inicia la JVM con configuración de perfiles, puerto y memoria
# spring.profiles.active: Aplica perfil desde ENV
# server.port: Puerto de la aplicación
# management.server.port: Puerto de actuator (mismo que app para simplificar)
# -jar: Ejecuta el JAR compilado
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$SERVER_PORT -Dmanagement.server.port=$SERVER_PORT -jar order-service.jar"]
