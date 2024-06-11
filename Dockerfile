FROM openjdk:23-jdk-slim

WORKDIR /app

COPY StudentsK.jar /app/micro.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/micro.jar"]