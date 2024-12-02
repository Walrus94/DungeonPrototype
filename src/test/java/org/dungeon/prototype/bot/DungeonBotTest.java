package org.dungeon.prototype.bot;

import lombok.SneakyThrows;
import lombok.val;
import org.dungeon.prototype.config.BotConfig;
import org.dungeon.prototype.config.TestConfig;
import org.dungeon.prototype.exception.ChatException;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.state.ChatStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {BotConfig.class, TestConfig.class})
@ActiveProfiles("test")
class DungeonBotTest {

    protected static final Long CHAT_ID = 123456789L;
    protected static final Integer MESSAGE_ID = 9867;

    @Autowired
    private DungeonBot dungeonBot;
    @MockBean
    private BotCommandHandler botCommandHandler;
    @MockBean
    private CallbackHandler callbackHandler;
    @MockBean
    private ChatStateService chatStateService;
    @MockBean
    private PlayerService playerService;

    @Test
    @SneakyThrows
    @DisplayName("Successfully processes registering new player")
    public void processNicknamePrompt() {
        val update = mock(Update.class);
        val message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getMessageId()).thenReturn(MESSAGE_ID);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("nickname");
        when(chatStateService.isAwaitingNickname(CHAT_ID)).thenReturn(true);
        doNothing().when(playerService).registerPlayerAndSendStartMessage(CHAT_ID, "nickname");

        dungeonBot.onWebhookUpdateReceived(update);

        verify(playerService).registerPlayerAndSendStartMessage(CHAT_ID, "nickname");
    }

    @Test
    @SneakyThrows
    @DisplayName("Successfully processes update with /start action message")
    void onUpdateReceived_startCommand_newUser() {
        val update = mock(Update.class);
        val message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getMessageId()).thenReturn(MESSAGE_ID);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        update.setMessage(message);
        when(chatStateService.isStartAvailable(CHAT_ID)).thenReturn(true);
        doNothing().when(botCommandHandler).processStartAction(CHAT_ID);


        dungeonBot.onWebhookUpdateReceived(update);

        verify(botCommandHandler).processStartAction(CHAT_ID);
    }

    @Test
    @SneakyThrows
    @DisplayName("Fails to process update with /start action message from non-authenticated user")
    void onUpdateReceived_startCommand_newUser_notAuthenticated() {
        val update = mock(Update.class);
        val message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(9999999L);
        when(message.getMessageId()).thenReturn(MESSAGE_ID);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        update.setMessage(message);
//        when(chatStateService.isStartAvailable(CHAT_ID)).thenReturn(true);
//        doNothing().when(botCommandHandler).processStartAction(CHAT_ID);


        assertThrows(ChatException.class, () -> dungeonBot.onWebhookUpdateReceived(update));

//        verify(botCommandHandler).processStartAction(CHAT_ID);
    }

    @Test
    @DisplayName("Successfully process map command during game")
    void onUpdateReceived_mapCommand_GameMenuAvailable() {
        val update = mock(Update.class);
        val message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getMessageId()).thenReturn(MESSAGE_ID);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/map");
        update.setMessage(message);
        when(chatStateService.isGameMenuAvailable(CHAT_ID)).thenReturn(true);
        doNothing().when(botCommandHandler).processStartAction(CHAT_ID);


        dungeonBot.onWebhookUpdateReceived(update);

        verify(botCommandHandler).processMapAction(CHAT_ID);
    }

    @Test
    @DisplayName("Successfully process map command during game")
    void onUpdateReceived_mapCommand_GameMenuUnavailable() {
        val update = mock(Update.class);
        val message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getMessageId()).thenReturn(MESSAGE_ID);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/map");
        update.setMessage(message);
        when(chatStateService.isGameMenuAvailable(CHAT_ID)).thenReturn(false);
        doNothing().when(botCommandHandler).processStartAction(CHAT_ID);


        dungeonBot.onWebhookUpdateReceived(update);

        verifyNoInteractions(botCommandHandler);
    }
}