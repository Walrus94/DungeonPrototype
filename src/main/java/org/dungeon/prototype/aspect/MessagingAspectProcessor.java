package org.dungeon.prototype.aspect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.dungeon.prototype.aspect.dto.InventoryItemResponseDto;
import org.dungeon.prototype.bot.DungeonBot;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.service.MessageService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.item.ItemService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Optional;

@Slf4j
@Aspect
@Component
public class MessagingAspectProcessor {
    @Autowired
    DungeonBot dungeonBot;
    @Autowired
    RoomService roomService;
    @Autowired
    MessageService messageService;
    @Autowired
    PlayerService playerService;

    @Autowired
    LevelService levelService;

    @Autowired
    ItemService itemService;

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendRoomMessage)", returning = "result")
    public void sendRoomMessage(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleSendingRoomMessage(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.AnswerCallback)", returning = "result")
    public void answerCallback(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleCallbackAnswer(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendTreasureMessage)", returning = "result")
    public void sendTreasureOrRoomMessage(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleTreasureMessage(joinPoint);
        } else {
            handleSendingRoomMessage(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendMerchantBuyMenuMessage)", returning = "result")
    public void sendMerchantBuyMenuMessage(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleMerchantBuyMenuMessage(joinPoint);
        } else {
            handleSendingRoomMessage(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendMerchantBuyItem)", returning = "result")
    public void sendMerchantBuyItemMessage(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleMerchantBuyItemMessage(joinPoint);
        } else {
            handleMerchantBuyMenuMessage(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendMerchantSellMenuMessage)", returning = "result")
    public void sendMerchantSellMenuMessage(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleMerchantSellMenuMessage(joinPoint);
        } else {
            handleSendingRoomMessage(joinPoint);
        }
    }
    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendMapMessage)", returning = "result")
    public void sendMapMessage(JoinPoint joinPoint, String result) {
        if (!result.isEmpty()) {
            handleSendingMapMessage(joinPoint, result);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendPlayerStatsMessage)", returning = "result")
    public void sendPlayerStatsMessage(JoinPoint joinPoint, boolean result) {
        if (result) {
            handlePlayerStatsMessage(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendInventoryMessage)", returning = "result")
    public void sendInventoryMessage(JoinPoint joinPoint, InventoryItemResponseDto result) {
        if (result.isOk()) {
            handleSendingInventoryMessage(joinPoint);
        } else {
            handleInventoryItemMessage(joinPoint, result);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.SendInventoryItem)", returning = "result")
    public void sendInventoryItemMessage(JoinPoint joinPoint, InventoryItemResponseDto result) {
        handleInventoryItemMessage(joinPoint, result);
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.PhotoMessageSending)", returning = "result")
    public void sendPhotoMessage(JoinPoint joinPoint, SendPhoto result) {
        handleSendingPhotoMessage(joinPoint, result);
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.MessageSending)", returning = "message")
    public void sendMessage(JoinPoint joinPoint, SendMessage message) {
        handleSendingMessage(joinPoint, message);
    }

    private void handleSendingPhotoMessage(JoinPoint joinPoint, SendPhoto result) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            dungeonBot.sendMessage(chatId, result);
        }
    }

    private void handleSendingInventoryMessage(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            messageService.sendInventoryMessage(chatId, player.getInventory());
        }
    }

    private void handleSendingMapMessage(JoinPoint joinPoint, String result) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            messageService.sendMapMenuMessage(chatId, result);
        }
    }

    private void handlePlayerStatsMessage(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            messageService.sendPlayerStatsMessage(chatId, player);
        }
    }

    private void handleInventoryItemMessage(JoinPoint joinPoint, InventoryItemResponseDto result) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val item = itemService.findItem(chatId, result.getItemId());
            messageService.sendItemInfoMessage(chatId, item, result.getInventoryType(), Optional.ofNullable(result.getItemType()));
        }
    }

    private void handleMerchantBuyItemMessage(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            if (args.length > 1 && args[1] instanceof String itemId) {
                val item = itemService.findItem(chatId, itemId);
                messageService.sendMerchantBuyItemMessage(chatId, item);
            }
        }
    }

    private void handleMerchantSellMenuMessage(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            messageService.sendMerchantSellMenuMessage(chatId, player);
        }
    }

    private void handleMerchantBuyMenuMessage(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            val merchant = (Merchant) roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId()).getRoomContent();
            messageService.sendMerchantBuyMenuMessage(chatId, player.getGold(), merchant.getItems());
        }
    }

    private void handleTreasureMessage(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
            if (currentRoom.getRoomContent() instanceof Treasure treasure) {
                if (treasure.getGold() == 0 && treasure.getItems().isEmpty()) {
                    log.info("Treasure looted!");
                    val level = levelService.getLevel(chatId);
                    levelService.updateAfterTreasureLooted(level, currentRoom);
                    messageService.sendRoomMessage(chatId, player, currentRoom);
                } else {
                    messageService.sendTreasureMessage(chatId, treasure);
                }
            } else {
                messageService.sendRoomMessage(chatId, player, currentRoom);
            }
        }
    }

    private void handleSendingRoomMessage(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            val player = playerService.getPlayer(chatId);
            val room = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
            messageService.sendRoomMessage(chatId, player, room);
        }
    }

    private void handleCallbackAnswer(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long) {
            if (args.length > 1 && args[1] instanceof CallbackQuery callbackQuery) {
                dungeonBot.answerCallbackQuery(callbackQuery.getId());
            }
        }
    }

    private void handleSendingMessage(JoinPoint joinPoint, SendMessage message) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            dungeonBot.sendMessage(chatId, message);
        }
    }
}
