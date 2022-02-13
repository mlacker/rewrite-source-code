#FROM maven:3.8-jdk-11 AS build
#WORKDIR /usr/src/app
#COPY pom.xml .
#RUN mvn -B dependency:resolve
#COPY src .
#RUN mvn clean package

FROM openjdk:11
WORKDIR /app
EXPOSE 8080
COPY target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
