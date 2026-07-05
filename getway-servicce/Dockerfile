FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/getway-servicce-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8058
ENTRYPOINT ["java", "-jar", "app.jar"]