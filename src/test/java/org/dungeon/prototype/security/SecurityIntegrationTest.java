package org.dungeon.prototype.security;

import lombok.SneakyThrows;
import lombok.val;
import org.dungeon.prototype.bot.DungeonBot;
import org.dungeon.prototype.bot.WebhookController;
import org.dungeon.prototype.config.TestConfig;
import org.dungeon.prototype.exception.ChatException;
import org.dungeon.prototype.service.message.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        TestConfig.class,
        WebhookController.class,
        DungeonBot.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
public class SecurityIntegrationTest {

    private static final long CHAT_ID = 123456789L;

    @LocalServerPort
    private int port;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/webhook";
    }
    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private DungeonBot dungeonBot;
    @MockBean
    private MessageService messageService;

    @Test
    @DisplayName("Unauthorized")
    void whenNotAuthenticated_thenUnauthorized() {
        val update = mock(Update.class);
        val message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(999999999L);
        HttpEntity<Update> request = new HttpEntity<>(update);

        assertThrows(ChatException.class, () ->
                restTemplate.postForEntity(getBaseUrl(), request, Void.class));

    }

    @Test
    @SneakyThrows
    @DisplayName("Authorized")
    void whenAuthenticated_thenAuthorized() {
        val update = mock(Update.class);
        val message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(CHAT_ID);
        HttpEntity<Update> request = new HttpEntity<>(update);

        restTemplate.postForEntity(getBaseUrl(), request, String.class);

        verify(dungeonBot).onWebhookUpdateReceived(eq(update));

    }
}
