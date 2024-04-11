package org.dungeon.prototype.config;

import org.dungeon.prototype.bot.DungeonBot;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.db.MapDBContext;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public DefaultBotOptions defaultBotOptions() {
        return new DefaultBotOptions();
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(DungeonBot dungeonBot) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(dungeonBot);
        return botsApi;
    }

    @Bean
    public DBContext dbContext() {
        DB db = DBMaker.fileDB("dungeonproto.db").make();
        return new MapDBContext(db);
    }
}
