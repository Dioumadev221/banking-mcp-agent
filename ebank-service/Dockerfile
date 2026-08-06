# ===== Étape 1 : BUILD (compile le jar) =====
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# ===== Étape 2 : RUNTIME (juste le JRE + le jar) =====
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8057
ENTRYPOINT ["java", "-jar", "app.jar"]