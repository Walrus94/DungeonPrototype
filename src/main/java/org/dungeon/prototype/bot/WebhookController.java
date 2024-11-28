package org.dungeon.prototype.bot;

import org.dungeon.prototype.exception.RestrictedOperationException;
import org.dungeon.prototype.properties.CallbackType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("${bot.path}")
public class WebhookController {

    private final DungeonBot dungeonBot;

    public WebhookController(DungeonBot dungeonBot) {
        this.dungeonBot = dungeonBot;
    }

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        long userId = update.getMessage().getFrom().getId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!authentication.isAuthenticated() || !authentication.getPrincipal().equals(userId)) {
            //TODO: implement exception message without button
            throw new RestrictedOperationException(userId, "authentificate",
                    "Unauthorized access. You are not allowed to use this bot.", CallbackType.MENU_BACK);
        }
        dungeonBot.onWebhookUpdateReceived(update);
    }
}
