FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/log-platform-1.0.0.jar app.jar
USER 10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
