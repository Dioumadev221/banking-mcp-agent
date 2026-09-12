# ---- Build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Dependencies are resolved from the POM alone and cached as their own layer,
# so editing source code does not re-download the world on every rebuild.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage ----
# JRE, not JDK: a smaller image with no compiler shipped to production.
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl is here only so Docker can probe the actuator health endpoint; it is
# what lets dependent services wait for readiness instead of guessing.
RUN apt-get update && apt-get install -y --no-install-recommends curl     && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
