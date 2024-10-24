# Use Gradle to build the DungeonPrototype app
FROM gradle:8.10.1 AS build

WORKDIR /app

# Copy the Gradle project files (to use the build cache when possible)
COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle
RUN gradle build --no-daemon || return 0  # Initial build to download dependencies

# Copy the rest of the source code
COPY . .

# Build the DungeonPrototype Spring Boot JAR
RUN gradle bootJar --no-daemon

# Use a lightweight JRE to run the Spring Boot application
FROM openjdk:23-ea-8-jdk-slim
COPY --from=build /app/build/libs/*.jar /app/DungeonPrototype.jar

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "/app/DungeonPrototype.jar"]
