# Use Gradle to build the DungeonPrototype app
FROM gradle:8.10.1 AS build

WORKDIR /app

ARG BOT_AUTH_TOKEN
ARG BOT_WEBHOOK_URL
ARG BOT_WEBHOOK_PATH
ARG BOT_WEBHOOK_PORT

ENV BOT_AUTH_TOKEN=$BOT_AUTH_TOKEN
ENV BOT_WEBHOOK_URL=$BOT_WEBHOOK_URL
ENV BOT_WEBHOOK_PATH=$BOT_WEBHOOK_PATH
ENV BOT_WEBHOOK_PORT=$BOT_WEBHOOK_PORT

# Set webhook url
RUN echo "Setting webhook for Telegram bot..." && \
    echo "Telegram API Response: $(curl -X POST https://api.telegram.org/bot$BOT_AUTH_TOKEN/setWebhook \
    -F url=${BOT_WEBHOOK_URL}${BOT_WEBHOOK_PATH} )";
# Copy the Gradle project files (to use the build cache when possible)
COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle
RUN gradle build --no-daemon || return 0  # Initial build to download dependencies

# Copy the rest of the source code
COPY . .

# Build the DungeonPrototype Spring Boot JAR
RUN gradle bootJar --no-daemon

# Use a lightweight JRE to run the Spring Boot application
FROM openjdk:21-ea-8-slim
COPY --from=build /app/build/libs/*.jar /app/DungeonPrototype.jar

# Run the Spring Boot app
ENTRYPOINT java -jar app/DungeonPrototype.jar
