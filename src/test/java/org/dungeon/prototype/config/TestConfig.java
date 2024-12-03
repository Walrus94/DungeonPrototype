package org.dungeon.prototype.config;

import org.dungeon.prototype.bot.DungeonBot;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi() {
        return mock(TelegramBotsApi.class);
    }

    @Bean
    public SetWebhook setWebhook() {
        return mock(SetWebhook.class);
    }

    @Bean
    public DungeonBot dungeonBot(SetWebhook setWebhook) {
        return new DungeonBot( "botUsername", "", "/webhook", setWebhook);
    }
}
