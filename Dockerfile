# =========================================================
# Stage 1: Build the JAR
# =========================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -Dspotless.check.skip=true -q

# =========================================================
# Stage 2: Run the JAR
# =========================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Tuned to fit a 512MB free-tier container. The previous MaxMetaspaceSize=80m /
# CompressedClassSpaceSize=32m was too tight for Spring Boot 4 + Hibernate + Spring
# Security + JPA + Redis + springdoc, which load a large number of classes and lambda
# proxies at runtime, and crashed the app with OutOfMemoryError: Metaspace shortly
# after startup.
ENTRYPOINT ["java", "-Xms32m", "-Xmx256m", "-XX:MaxMetaspaceSize=200m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
