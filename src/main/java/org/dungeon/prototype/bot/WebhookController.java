package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.exception.ChatException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequestMapping("${bot.path}")
public class WebhookController {
    private static final String AUTH_EXCEPTION_MESSAGE = "Unauthorized access. This bot is for development purposes only. " +
            "Contact @arsnazarov for more info";
    private final DungeonBot dungeonBot;

    public WebhookController(DungeonBot dungeonBot) {
        this.dungeonBot = dungeonBot;
    }

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        long chatId = update.getMessage().getChatId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!authentication.isAuthenticated() || !authentication.getPrincipal().equals(chatId)) {
            log.info("Authentication failed for chatId:{}!", chatId);
            throw new ChatException(AUTH_EXCEPTION_MESSAGE, chatId);
        } else {
            log.info("Successfully authenticated user, chatId:{}", chatId);
            dungeonBot.onWebhookUpdateReceived(update);
        }
    }
}
