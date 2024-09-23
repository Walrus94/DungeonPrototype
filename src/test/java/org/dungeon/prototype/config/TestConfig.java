package org.dungeon.prototype.config;

import org.dungeon.prototype.bot.DungeonBot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {
    @Bean
    @Qualifier("botToken")
    public String botToken() {
        return "test-bot-token";
    }

    @Bean
    @Qualifier("botUsername")
    public String botUsername() {
        return "test-bot-username";
    }

    @Bean
    @Primary
    public TelegramBotsApi telegramBotsApi() {
        // Mock the TelegramBotsApi to avoid actual Telegram API calls
        return mock(TelegramBotsApi.class);
    }

    @Bean
    @Primary
    public DungeonBot dungeonBot(String botToken, String botUsername) {
        return new DungeonBot(botToken, botUsername);
    }
}
