package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequestMapping("${bot.path}")
public class WebhookController {
    private final DungeonBot dungeonBot;

    public WebhookController(DungeonBot dungeonBot) {
        this.dungeonBot = dungeonBot;
    }

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        dungeonBot.onWebhookUpdateReceived(update);
    }
}
