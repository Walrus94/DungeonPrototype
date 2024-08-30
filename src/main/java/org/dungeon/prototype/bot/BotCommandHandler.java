package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.InitializeChatContext;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotCommandHandler {

    @Autowired
    PlayerService playerService;
    @Autowired
    LevelService levelService;
    @Autowired
    MessageService messageService;
    @InitializeChatContext
    public void processStartAction(Long chatId, String suggestedNickname) {
        if (!playerService.hasPlayer(chatId)) {
            messageService.sendRegisterMessage(chatId, suggestedNickname);
        } else {
            val hasSavedGame = levelService.hasLevel(chatId);
            val nickname = playerService.getNicknameByChatId(chatId).orElse("stranger");
            messageService.sendContinueMessage(chatId, nickname, hasSavedGame);
        }
    }
}
