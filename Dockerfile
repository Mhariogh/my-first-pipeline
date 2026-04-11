FROM openjdk:17-slim

WORKDIR /app

COPY target/my-first-pipeline-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
