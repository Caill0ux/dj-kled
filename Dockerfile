FROM maven:4.0.0-openjdk-11 AS build

COPY pom.xml /app/pom.xml
COPY src /app/src

WORKDIR /app

RUN mvn dependency:go-offline
RUN mvn package

FROM openjdk:11

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
