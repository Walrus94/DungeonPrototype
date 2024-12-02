package org.dungeon.prototype.config;

import org.dungeon.prototype.bot.DungeonBot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class BotConfig {

    private final String botToken;
    private final String botUsername;
    private final String webHookUrl;
    private final String botPath;
    private final List<Long> authUsers;

    public BotConfig(@Value("${bot.token}") String botToken,
                     @Value("${bot.username}") String botUsername,
                     @Value("${bot.webhook}") String webHookUrl,
                     @Value("${bot.path}") String botPath,
                     @Value("${auth-users}") String authUsers) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.webHookUrl = webHookUrl;
        this.botPath = botPath;
        this.authUsers = authUsers.isEmpty() ? Collections.emptyList() :
                Arrays.stream(authUsers.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    @Bean
    @Qualifier("authUsers")
    public List<Long> authUsers() {
        return authUsers;
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
    public DungeonBot dungeonBot(String botUsername, String botToken, String botPath, SetWebhook setWebhook, List<Long> authUsers) {
        return new DungeonBot(botUsername, botToken, botPath, setWebhook, authUsers);
    }
}
