FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/ebank-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8057
ENTRYPOINT ["java", "-jar", "app.jar"]