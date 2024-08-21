package org.dungeon.prototype;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.meta.generics.LongPollingBot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class BaseUnitTest {
    @MockBean
    private TelegramBotsApi telegramBotsApi;
    @MockBean
    private BotSession botSession;

    @SneakyThrows
    @BeforeEach
    public void initMocks() {
        when(telegramBotsApi.registerBot(any(LongPollingBot.class))).thenReturn(botSession);
    }
}
