package org.dungeon.prototype.bot;

import lombok.val;
import org.dungeon.prototype.exception.ChatException;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static java.util.Objects.isNull;

@RestController
@RequestMapping("${bot.path}")
public class WebhookController {

    private static final String EXCEPTION_MESSAGE = "Unauthorized access. This bot is for development purposes only. Contact @arsnazarov for more info";

    @Value("${auth-users}")
    List<Long> authUsers;

    private final DungeonBot dungeonBot;

    public WebhookController(DungeonBot dungeonBot) {
        this.dungeonBot = dungeonBot;
    }

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        val message = update.getMessage();
        if (isNull(message)) {
            throw new DungeonPrototypeException(EXCEPTION_MESSAGE);
        }
        if (authUsers.isEmpty() || authUsers.contains(message.getChatId())) {
            dungeonBot.onWebhookUpdateReceived(update);
        } else {
            throw new ChatException(EXCEPTION_MESSAGE , message.getChatId());
        }
    }
}
