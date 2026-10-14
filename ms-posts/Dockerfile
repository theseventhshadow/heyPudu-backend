# ============================================================
# Etapa 1: Build
# ============================================================
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew build -x test --no-daemon

# ============================================================
# Etapa 2: Runtime
# ============================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd -r heypudu && useradd -r -g heypudu heypudu

COPY --from=build /app/build/libs/*.jar app.jar

USER heypudu

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]