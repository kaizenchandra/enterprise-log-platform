FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B package
FROM eclipse-temurin:21-jre
COPY --from=build /src/target/demo-1.0.0.jar /app.jar
USER 10001
ENTRYPOINT ["java","-jar","/app.jar"]
