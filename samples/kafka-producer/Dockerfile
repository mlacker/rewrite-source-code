FROM maven:3.8-jdk-11 AS build
WORKDIR /usr/src/app
COPY pom.xml .
RUN mvn -B dependency:resolve
COPY src .
RUN mvn clean package

FROM openjdk:11
WORKDIR /usr/lib/app
COPY --from=build /usr/src/app/target/app.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar app.jar"]
