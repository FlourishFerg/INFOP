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

# -Xmx256m keeps memory within Koyeb's 512MB free tier limit
ENTRYPOINT ["java", "-Xms16m", "-Xmx128m", "-XX:MaxMetaspaceSize=80m", "-XX:+UseSerialGC", "-XX:CompressedClassSpaceSize=32m", "-XX:TieredStopAtLevel=1", "-noverify", "-jar", "app.jar"]
