FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/my-first-pipeline-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
