# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline -DskipTests
COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:25-jre-alpine

COPY --from=build /src/target/platform-api-*.jar /app/platform-api.jar
USER 65532:65532
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=6 \
  CMD ["wget", "-qO-", "http://127.0.0.1:8080/actuator/health/liveness"]
ENTRYPOINT ["java", "-jar", "/app/platform-api.jar"]
