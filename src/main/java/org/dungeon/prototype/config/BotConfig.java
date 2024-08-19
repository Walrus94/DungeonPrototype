package org.dungeon.prototype.config;

import org.dungeon.prototype.bot.DungeonBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    private final String botToken;

    private final String botUsername;

    public BotConfig(@Value("${bot.token}") String botToken,
                     @Value("${bot.username}") String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Bean
    public String botToken() {
        return botToken;
    }

    @Bean
    public String botUsername() {
        return botUsername;
    }


    @Bean
    public TelegramBotsApi telegramBotsApi(DungeonBot dungeonBot) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(dungeonBot);
        return botsApi;
    }

    @Bean
    public DungeonBot DungeonBot(String botToken, String botUsername) {
        return new DungeonBot(botToken, botUsername);
    }
}
