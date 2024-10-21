package org.dungeon.prototype.config;

import org.dungeon.prototype.bot.DungeonBot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;

@Configuration
public class BotConfig {

    private final String botToken;
    private final String botUsername;
    private final String webHookUrl;
    private final String botPath;
    public BotConfig(@Value("${bot.token}") String botToken,
                     @Value("${bot.username}") String botUsername,
                     @Value("${bot.webhook}") String webHookUrl,
                     @Value("${bot.path}") String botPath) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.webHookUrl = webHookUrl;
        this.botPath = botPath;
    }

    @Bean
    @Qualifier("botToken")
    public String botToken() {
        return botToken;
    }

    @Bean
    @Qualifier("botUsername")
    public String botUsername() {
        return botUsername;
    }

    @Bean
    @Qualifier("webHookUrl")
    public String webHookUrl() {
        return webHookUrl;
    }

    @Bean
    @Qualifier("botPath")
    public String botPath() {
        return botPath;
    }

    @Bean
    public SetWebhook setWebhook(String webHookUrl, String botPath) {
        return SetWebhook.builder()
                .dropPendingUpdates(true)
                .url(webHookUrl + botPath)
                .build();
    }

    @Bean
    public DungeonBot dungeonBot(String botUsername,String botToken, String botPath, SetWebhook setWebhook) {
        return new DungeonBot(botUsername, botToken, botPath, setWebhook);
    }
}
