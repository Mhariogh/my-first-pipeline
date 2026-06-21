FROM eclipse-temurin:17-jre

WORKDIR /usr/src/app

# The JAR is produced by the pipeline's Package stage before this image
# is built. The app listens on port 8080 inside the container.
COPY target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
