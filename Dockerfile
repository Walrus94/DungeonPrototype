# Use Gradle to build the DungeonPrototype app
FROM gradle:8.10.1 AS build

WORKDIR /app

ARG BOT_AUTH_TOKEN
ARG BOT_WEBHOOK_URL
ARG BOT_WEBHOOK_PATH

ENV BOT_AUTH_TOKEN=$BOT_AUTH_TOKEN
ENV BOT_WEBHOOK_URL=$BOT_WEBHOOK_URL
ENV BOT_WEBHOOK_PATH=$BOT_WEBHOOK_PATH

# Set webhook url
RUN echo "Initializing webhook..." && \
    if && [ -n "$BOT_WEBHOOK_URL" ] && [-n "$BOT_WEBHOOK_PATH"]; then \
      echo "Setting webhook for Telegram bot at $FULL_WEBHOOK_URL..." && \
      echo "Telegram API Response: "$(curl -F 'url=${BOT_WEBHOOK_URL}${BOT_WEBHOOK_PATH}' https://api.telegram.org/bot$BOT_AUTH_TOKEN/setWebhook); \
    else \
      echo "BOT_TOKEN, WEBHOOK_URL, or WEBHOOK_PATH is missing. Skipping webhook setup."; \
      return -1 \
    fi

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
ENTRYPOINT java -jar app/DungeonPrototype.jar
