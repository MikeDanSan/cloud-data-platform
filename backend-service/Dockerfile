# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and build files first for better layer caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Copy source
COPY src ./src

# Build a runnable jar (skip tests for faster local iteration; CI will run tests)
RUN ./gradlew clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# Optional: default JVM settings (safe baseline)
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
