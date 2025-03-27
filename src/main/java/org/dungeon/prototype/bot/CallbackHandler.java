package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.AnswerCallback;
import org.dungeon.prototype.async.AsyncJobHandler;
import org.dungeon.prototype.async.TaskType;
import org.dungeon.prototype.exception.CallbackParsingException;
import org.dungeon.prototype.exception.RestrictedOperationException;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.content.MonsterRoom;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.KeyboardButtonProperties;
import org.dungeon.prototype.service.BattleService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.effect.EffectService;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.dungeon.prototype.service.item.generation.ItemGenerator;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.service.room.TreasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Map;
import java.util.Optional;

import static org.dungeon.prototype.properties.CallbackType.BOOTS;
import static org.dungeon.prototype.properties.CallbackType.GLOVES;
import static org.dungeon.prototype.properties.CallbackType.HEAD;
import static org.dungeon.prototype.properties.CallbackType.INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.LEFT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MENU_BACK;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_MENU;
import static org.dungeon.prototype.properties.CallbackType.RIGHT_HAND;
import static org.dungeon.prototype.properties.CallbackType.VEST;
import static org.dungeon.prototype.util.LevelUtil.getDirectionSwitchByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getErrorMessageByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getNextPointInDirection;

@Slf4j
@Component
public class CallbackHandler {
    @Autowired
    BotCommandHandler botCommandHandler;
    @Autowired
    AsyncJobHandler asyncJobHandler;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ItemGenerator itemGenerator;
    @Autowired
    private EffectService effectService;
    @Autowired
    private BattleService battleService;
    @Autowired
    private TreasureService treasureService;
    @Autowired
    private KeyboardButtonProperties keyboardButtonProperties;

    /**
     * Handles callbacks from incoming updates
     * and executes corresponding services methods
     *
     * @param chatId        id of updated chat
     * @param callbackQuery query with callback data
     */
    @AnswerCallback
    public void handleCallbackQuery(Long chatId, CallbackQuery callbackQuery) {
        asyncJobHandler.submitCallbackTask(() -> {
            val callData = callbackQuery.getData();
            try {
                val callBackData = getCallbackType(callData);

                switch (callBackData) {
                    case START_GAME -> handleStartingNewGame(chatId);
                    case CONTINUE_GAME -> handleContinuingGame(chatId);
                    case NEXT_LEVEL -> handleNextLevel(chatId);
                    case LEFT, RIGHT, FORWARD, BACK -> handleMovingToRoom(chatId, callBackData);
                    case ATTACK, SECONDARY_ATTACK -> handleAttack(chatId, callBackData);
                    case TREASURE_OPEN -> handleOpeningTreasure(chatId);
                    case TREASURE_GOLD_COLLECTED -> handleCollectingTreasureGold(chatId);
                    case SHRINE -> handleShrineRefill(chatId);
                    case MERCHANT_BUY_MENU, MERCHANT_BUY_MENU_BACK -> handleOpenMerchantBuyMenu(chatId);
                    case MERCHANT_SELL_MENU, MERCHANT_SELL_MENU_BACK -> handleOpenMerchantSellMenu(chatId);
                    case MAP -> handleSendingMapMessage(chatId);
                    case INVENTORY, ITEM_INVENTORY_BACK -> handleSendingInventoryMessage(chatId);
                    case PLAYER_STATS -> playerService.sendPlayerStatsMessage(chatId);
                    case MENU_BACK -> handleSendingRoomMessage(chatId);
                    case TREASURE_COLLECT_ALL -> handleCollectingTreasure(chatId);
                    case RESTORE_ARMOR -> roomService.restoreArmor(chatId);
                    case SHARPEN_WEAPON -> inventoryService.sharpenWeapon(chatId);
                }
            } catch (CallbackParsingException e) {
                log.warn("Processing composite callback, parsing failed: {}", e.getMessage());
                parseCompositeCallbackData(chatId, callData);
            }
        });
    }

    private void parseCompositeCallbackData(Long chatId, String callData) {
        if (callData.startsWith("btn_treasure_item_collected_")) {
            val itemId = callData.replaceFirst("^" + "btn_treasure_item_collected_", "");
            treasureService.collectTreasureItem(chatId, itemId);
        }
        if (callData.startsWith("btn_inventory_display_")) {
            if (callData.startsWith("btn_inventory_display_item_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_display_item_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.empty());
            }

            if (callData.startsWith("btn_inventory_display_head_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_display_head_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(HEAD));
            }
            if (callData.startsWith("btn_inventory_display_vest_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_display_vest_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(VEST));
            }
            if (callData.startsWith("btn_inventory_display_gloves_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_display_gloves_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(GLOVES));
            }
            if (callData.startsWith("btn_inventory_display_boots_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_display_boots_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(BOOTS));
            }
            if (callData.startsWith("btn_inventory_display_primary_weapon_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_display_primary_weapon_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(RIGHT_HAND));
            }
            if (callData.startsWith("btn_inventory_display_secondary_weapon_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_display_secondary_weapon_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(LEFT_HAND));
            }
        }
        if (callData.startsWith("btn_inventory_item_")) {
            if (callData.startsWith("btn_inventory_item_equip_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_item_equip_", "");
                inventoryService.equipItem(chatId, itemId);
            }
            if (callData.startsWith("btn_inventory_item_un_equip_")) {
                val itemId = callData.replaceFirst("^" + "btn_inventory_item_un_equip_", "");
                inventoryService.unEquipItem(chatId, itemId);
            }
        }
        if (callData.startsWith("btn_merchant_")) {

            if (callData.startsWith("btn_merchant_list_item_sell_")) {
                val itemId = callData.replaceFirst("^" + "btn_merchant_list_item_sell_", "");
                inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.empty());
            }

            if (callData.startsWith("btn_merchant_list_item_buy_")) {
                val itemId = callData.replaceFirst("^" + "btn_merchant_list_item_buy_", "");
                roomService.openMerchantBuyItem(chatId, itemId);
            }
            if (callData.startsWith("btn_merchant_sell_")) {
                val itemId = callData.replaceFirst("^" + "btn_merchant_sell_", "");
                inventoryService.sellItem(chatId, itemId);
            }
            if (callData.startsWith("btn_merchant_buy_")) {
                val itemId = callData.replaceFirst("^" + "btn_merchant_buy_", "");
                inventoryService.buyItem(chatId, itemId);
            }

            if (callData.startsWith("btn_merchant_display_")) {
                if (callData.startsWith("btn_merchant_display_item_")) {
                    val itemId = callData.replaceFirst("^" + "btn_merchant_display_item_", "");
                    inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.empty());
                }
                if (callData.startsWith("btn_merchant_display_head_")) {
                    val itemId = callData.replaceFirst("^" + "btn_merchant_display_head_", "");
                    inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(HEAD));
                }
                if (callData.startsWith("btn_merchant_display_vest_")) {
                    val itemId = callData.replaceFirst("^" + "btn_merchant_display_vest_", "");
                    inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(VEST));
                }
                if (callData.startsWith("btn_merchant_display_gloves_")) {
                    val itemId = callData.replaceFirst("^" + "btn_merchant_display_gloves_", "");
                    inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(GLOVES));
                }
                if (callData.startsWith("btn_merchant_display_boots_")) {
                    val itemId = callData.replaceFirst("^" + "btn_merchant_display_boots_", "");
                    inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(BOOTS));
                }
                if (callData.startsWith("btn_merchant_display_primary_weapon_")) {
                    val itemId = callData.replaceFirst("^" + "btn_merchant_display_primary_weapon_", "");
                    inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(RIGHT_HAND));
                }
                if (callData.startsWith("btn_merchant_display_secondary_weapon_")) {
                    val itemId = callData.replaceFirst("^" + "btn_merchant_display_secondary_weapon_", "");
                    inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(LEFT_HAND));
                }
            }
        }
        if (callData.startsWith("btn_player_attribute_upgrade_")) {
            val playerAttribute = PlayerAttribute.fromValue(callData.replaceFirst("^" + "btn_player_attribute_upgrade_", ""));
            roomService.upgradePlayerAttribute(chatId, playerAttribute);
        }
        //TODO: add parsing exception
    }

    private void handleStartingNewGame(Long chatId) {
        itemGenerator.generateItems(chatId);
        asyncJobHandler.submitTask(() -> {
            val defaultInventory = inventoryService.getDefaultInventory(chatId);
            var player = playerService.getPlayerPreparedForNewGame(chatId, defaultInventory);
            player = effectService.updatePlayerEffects(player);
            player = effectService.updateArmorEffect(player);
            inventoryService.saveOrUpdateInventory(defaultInventory);
            playerService.updatePlayer(player);
        }, TaskType.PREPARE_PLAYER, chatId);
        levelService.startNewGame(chatId);
    }

    private void handleContinuingGame(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        levelService.continueGame(chatId, player, currentRoom);
    }

    private void handleNextLevel(Long chatId) {
        var player = playerService.getPlayer(chatId);
        levelService.nextLevel(chatId, player);
    }

    private void handleSendingRoomMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        roomService.sendRoomMessage(chatId, player);
    }

    private void handleOpeningTreasure(Long chatId) {
        val player = playerService.getPlayer(chatId);
        treasureService.openTreasure(chatId, player);
    }

    private void handleCollectingTreasure(Long chatId) {
        var player = playerService.getPlayer(chatId);
        var currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        treasureService.collectAllTreasure(chatId, player, currentRoom);
    }

    private void handleCollectingTreasureGold(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        treasureService.collectTreasureGold(chatId, player, currentRoom);
    }

    private void handleMovingToRoom(Long chatId, CallbackType callBackData) {
        var player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        val newDirection = getDirectionSwitchByCallBackData(player.getDirection(), callBackData);
        if (!currentRoom.getAdjacentRooms().containsKey(newDirection) || !currentRoom.getAdjacentRooms().get((newDirection))) {
            throw new RestrictedOperationException(chatId, "move to room", getErrorMessageByCallBackData(callBackData), MENU_BACK);
        }
        val nextRoom = levelService.getRoomByChatIdAndCoordinates(chatId, getNextPointInDirection(currentRoom.getPoint(), newDirection));
        log.info("Moving to {} door: {}", callBackData.toString().toLowerCase(), nextRoom.getPoint());
        levelService.moveToRoom(chatId, player, nextRoom, newDirection);
    }

    private void handleShrineRefill(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        levelService.shrineUsage(chatId, player, currentRoom, level);
    }

    private void handleAttack(Long chatId, CallbackType callBackData) {
        var player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (!(currentRoom.getRoomContent() instanceof MonsterRoom)) {
            throw new RestrictedOperationException(chatId, "attack", "No monster to attack!", MENU_BACK);
        }
        battleService.attack(chatId, player, currentRoom, callBackData);
    }

    private void handleOpenMerchantBuyMenu(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        roomService.openMerchantBuyMenu(chatId, player, currentRoom);
    }

    private void handleOpenMerchantSellMenu(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        roomService.openMerchantSellMenu(chatId, player, currentRoom);
    }

    private void handleSendingMapMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        levelService.sendMapMessage(chatId, player);
    }

    private void handleSendingInventoryMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        inventoryService.sendInventoryMessage(chatId, player);
    }

    private CallbackType getCallbackType(String callbackData) {
        return keyboardButtonProperties.getButtons().entrySet().stream()
                .filter(entry -> callbackData.equals(entry.getValue().getCallback()))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(() ->
                        new CallbackParsingException(callbackData));
    }
}
