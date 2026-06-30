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

# Tuned to fit a 400MB container (Railway trial). Xmx256m + MaxMetaspaceSize200m
# alone already exceeds 400MB before any other JVM overhead (code cache, thread
# stacks, native memory), which is why that budget still crashed with
# OutOfMemoryError: Metaspace under real request load. Heap is trimmed down since
# this is a small CRUD API that doesn't need a large heap, leaving more of the
# budget for metaspace (Spring Boot 4 + Hibernate + Spring Security + JPA + Redis +
# springdoc load a lot of classes/lambda proxies). Code cache and thread stack size
# are also capped since both add up under concurrent request load.
ENTRYPOINT ["java", "-Xss256k", "-Xms16m", "-Xmx128m", "-XX:MaxMetaspaceSize=180m", "-XX:ReservedCodeCacheSize=32m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Dserver.tomcat.threads.max=10", "-jar", "app.jar"]
