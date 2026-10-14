# ============================================================
# Etapa 1: Build
# ============================================================
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copiar el wrapper de Gradle y los archivos de configuracion
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Dar permisos de ejecucion al wrapper
RUN chmod +x gradlew

# Descargar dependencias (capa cacheada)
RUN ./gradlew dependencies --no-daemon

# Copiar el codigo fuente y construir el JAR
COPY src src
RUN ./gradlew build -x test --no-daemon

# ============================================================
# Etapa 2: Runtime
# ============================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Crear usuario no root por seguridad
RUN groupadd -r heypudu && useradd -r -g heypudu heypudu

# Copiar el JAR construido en la etapa anterior
COPY --from=build /app/build/libs/*.jar app.jar

# Cambiar al usuario no root
USER heypudu

# Puerto del microservicio
EXPOSE 8081

# Punto de entrada
ENTRYPOINT ["java", "-jar", "app.jar"]