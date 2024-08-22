package org.dungeon.prototype;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.generics.BotSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


public class BaseUnitTest {
    @MockBean
    private TelegramBotsApi telegramBotsApi;
    @MockBean
    private BotSession botSession;

    @SneakyThrows
    @BeforeEach
    public void initMocks() {
        when(telegramBotsApi.registerBot(any(TelegramLongPollingBot.class))).thenReturn(botSession);
    }
}
