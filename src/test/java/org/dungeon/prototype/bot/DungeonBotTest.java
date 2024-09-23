package org.dungeon.prototype.bot;

import lombok.SneakyThrows;
import lombok.val;
import org.dungeon.prototype.config.BotConfig;
import org.dungeon.prototype.config.TestConfig;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {BotConfig.class, TestConfig.class})
@ActiveProfiles("test")
class DungeonBotTest {

    protected static final Long CHAT_ID = 123456789L;

    @Autowired
    private DungeonBot dungeonBot;
    @MockBean
    private BotCommandHandler botCommandHandler;
    @MockBean
    private CallbackHandler callbackHandler;
    @MockBean
    private PlayerService playerService;

    @SneakyThrows
    @Test
    @DisplayName("Successfully processes update with /start action message")
    void onUpdateReceived_startCommand_newUser() {
        val update = mock(Update.class);
        val message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        doNothing().when(botCommandHandler).processStartAction(CHAT_ID);

        dungeonBot.onUpdateReceived(update);

        verify(botCommandHandler).processStartAction(CHAT_ID);
    }

    @Test
    @DisplayName("Successfully processes registering new player")
    public void processNicknamePrompt() {
        val update = mock(Update.class);
        val message = mock(Message.class);
        val player = mock(Player.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("nickname");
        doNothing().when(playerService).registerPlayerAndSendStartMessage(CHAT_ID, "nickname");

        dungeonBot.onUpdateReceived(update);

        verify(playerService).registerPlayerAndSendStartMessage(CHAT_ID, "nickname");
    }
}