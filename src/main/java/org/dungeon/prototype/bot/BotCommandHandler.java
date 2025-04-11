package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.InitializeChatContext;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.state.ChatStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotCommandHandler {

    @Autowired
    PlayerService playerService;
    @Autowired
    LevelService levelService;
    ChatStateService chatStateService;
    @Autowired
    ItemService itemService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    MessageService messageService;

    /**
     * (Re-)Starts chat, initializes chat context and sends welcome message
     *
     * @param chatId id of the chat
     */
    @InitializeChatContext
    public void processStartAction(long chatId) {
        if (!playerService.hasPlayer(chatId)) {
            messageService.sendRegisterMessage(chatId);
        } else {
            val hasSavedGame = levelService.hasLevel(chatId);
            val nickname = playerService.getNicknameByChatId(chatId);
            messageService.sendStartMessage(chatId, nickname, hasSavedGame);
        }
    }

    public void processMapAction(long chatId) {
        if (playerService.hasPlayer(chatId)) {
            val player = playerService.getPlayer(chatId);
            levelService.sendMapMessage(chatId, player);
        }
    }

    public void processInventoryAction(long chatId) {
        if (playerService.hasPlayer(chatId)) {
            val player = playerService.getPlayer(chatId);
            inventoryService.sendInventoryMessage(chatId, player);
        }
    }

    public void processStatsAction(long chatId) {
        playerService.sendPlayerStatsMessage(chatId);
    }

    public void processStopAction(long chatId) {
        chatStateService.clearChatContext(chatId);
    }
}
