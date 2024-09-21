package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.AnswerCallback;
import org.dungeon.prototype.model.player.PlayerAttribute;
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
import java.util.Objects;
import java.util.Optional;

import static org.dungeon.prototype.properties.CallbackType.BOOTS;
import static org.dungeon.prototype.properties.CallbackType.GLOVES;
import static org.dungeon.prototype.properties.CallbackType.HEAD;
import static org.dungeon.prototype.properties.CallbackType.INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.LEFT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_MENU;
import static org.dungeon.prototype.properties.CallbackType.RIGHT_HAND;
import static org.dungeon.prototype.properties.CallbackType.VEST;
import static org.dungeon.prototype.util.LevelUtil.getDirectionSwitchByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getErrorMessageByCallBackData;
import static org.dungeon.prototype.util.LevelUtil.getNextPointInDirection;
import static org.dungeon.prototype.util.RoomGenerationUtils.getMonsterRoomTypes;

@Slf4j
@Component
public class CallbackHandler {
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
     * @param chatId id of updated chat
     * @param callbackQuery query with callback data
     * @return true if callback handled successfully
     */
    @AnswerCallback
    public boolean handleCallbackQuery(Long chatId, CallbackQuery callbackQuery) {
        val callData = callbackQuery.getData();
        val callBackData = getCallbackType(callData)
                .orElse(CallbackType.DEFAULT);

        return switch (callBackData) {
            case START_GAME ->
                    handleStartingNewGame(chatId);
            case CONTINUE_GAME ->
                    handleContinuingGame(chatId);
            case NEXT_LEVEL ->
                    handleNextLevel(chatId);
            case LEFT, RIGHT, FORWARD, BACK ->
                    handleMovingToRoom(chatId, callBackData);
            case ATTACK, SECONDARY_ATTACK ->
                    handleAttack(chatId, callBackData);
            case TREASURE_OPEN ->
                    handleOpeningTreasure(chatId);
            case TREASURE_GOLD_COLLECTED ->
                    handleCollectingTreasureGold(chatId);
            case SHRINE ->
                    handleShrineRefill(chatId);
            case MERCHANT_BUY_MENU, MERCHANT_BUY_MENU_BACK ->
                    handleOpenMerchantBuyMenu(chatId);
            case MERCHANT_SELL_MENU, MERCHANT_SELL_MENU_BACK ->
                    handleOpenMerchantSellMenu(chatId);
            case MAP ->
                    handleSendingMapMessage(chatId);
            case INVENTORY ->
                    handleSendingInventoryMessage(chatId);
            case PLAYER_STATS ->
                    playerService.sendPlayerStatsMessage(chatId);
            case MENU_BACK ->
                    handleSendingRoomMessage(chatId);
            case TREASURE_COLLECT_ALL ->
                    handleCollectingTreasure(chatId);
            case RESTORE_ARMOR -> roomService.restoreArmor(chatId);
            case SHARPEN_WEAPON -> true;
            //TODO: extract and refactor
            default -> {
                if (callData.startsWith("btn_treasure_item_collected_")) {
                    val itemId = callData.replaceFirst("^" + "btn_treasure_item_collected_", "");
                    yield treasureService.collectTreasureItem(chatId, itemId);
                }
                if (callData.startsWith("btn_inventory_display_")) {
                    if (callData.startsWith("btn_inventory_display_item_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_item_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.empty());
                    }

                    if (callData.startsWith("btn_inventory_display_head_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_head_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(HEAD));
                    }
                    if (callData.startsWith("btn_inventory_display_vest_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_vest_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(VEST));
                    }
                    if (callData.startsWith("btn_inventory_display_gloves_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_gloves_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(GLOVES));
                    }
                    if (callData.startsWith("btn_inventory_display_boots_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_boots_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(BOOTS));
                    }
                    if (callData.startsWith("btn_inventory_display_primary_weapon_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_primary_weapon_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(RIGHT_HAND));
                    }
                    if (callData.startsWith("btn_inventory_display_secondary_weapon_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_secondary_weapon_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(LEFT_HAND));
                    }
                }
                if (callData.startsWith("btn_inventory_item_")) {
                    if (callData.startsWith("btn_inventory_item_equip_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_item_equip_", "");
                        yield inventoryService.equipItem(chatId, itemId);
                    }
                    if (callData.startsWith("btn_inventory_item_un_equip_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_item_un_equip_", "");
                        yield inventoryService.unEquipItem(chatId, itemId);
                    }
                }
                if (callData.startsWith("btn_merchant_")) {

                    if (callData.startsWith("btn_merchant_list_item_sell_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_list_item_sell_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.empty());
                    }

                    if (callData.startsWith("btn_merchant_list_item_buy_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_list_item_buy_", "");
                        yield roomService.openMerchantBuyItem(chatId, itemId);
                    }
                    if (callData.startsWith("btn_merchant_sell_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_sell_", "");
                        yield inventoryService.sellItem(chatId, itemId);
                    }
                    if (callData.startsWith("btn_merchant_buy_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_buy_", "");
                        yield inventoryService.buyItem(chatId, itemId);
                    }

                    if (callData.startsWith("btn_merchant_display_")) {
                        if (callData.startsWith("btn_merchant_display_item_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_item_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.empty());
                        }
                        if (callData.startsWith("btn_merchant_display_head_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_head_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(HEAD));
                        }
                        if (callData.startsWith("btn_merchant_display_vest_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_vest_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(VEST));
                        }
                        if (callData.startsWith("btn_merchant_display_gloves_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_gloves_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(GLOVES));
                        }
                        if (callData.startsWith("btn_merchant_display_boots_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_boots_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(BOOTS));
                        }
                        if (callData.startsWith("btn_merchant_display_primary_weapon_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_primary_weapon_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(RIGHT_HAND));
                        }
                        if (callData.startsWith("btn_merchant_display_secondary_weapon_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_secondary_weapon_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(LEFT_HAND));
                        }
                    }
                }
                if (callData.startsWith("btn_player_attribute_upgrade_")) {
                    val playerAttribute = PlayerAttribute.fromValue(callData.replaceFirst("^" + "btn_player_attribute_upgrade_", ""));
                    yield roomService.upgradePlayerAttribute(chatId, playerAttribute);
                }
                yield false;
            }
        };
    }

    private boolean handleStartingNewGame(Long chatId) {
        itemGenerator.generateItems(chatId);
        val defaultInventory = inventoryService.getDefaultInventory(chatId);
        var player = playerService.getPlayerPreparedForNewGame(chatId, defaultInventory);
        player = effectService.updatePlayerEffects(player);
        player = effectService.updateArmorEffect(player);
        log.debug("Player loaded: {}", player);
        return levelService.startNewGame(chatId, player);
    }

    private boolean handleContinuingGame(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Couldn't find current room by id: {}", player.getCurrentRoomId());
            return false;
        }
        return levelService.continueGame(chatId, player, currentRoom);
    }

    private boolean handleNextLevel(Long chatId) {
        var player = playerService.getPlayer(chatId);
        levelService.nextLevel(chatId, player);
        return false;
    }

    private boolean handleSendingRoomMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        return roomService.sendOrUpdateRoomMessage(chatId, player);
    }

    private boolean handleOpeningTreasure(Long chatId) {
        val player = playerService.getPlayer(chatId);
        treasureService.openTreasure(chatId, player);
        return false;
    }

    private boolean handleCollectingTreasure(Long chatId) {
        var player = playerService.getPlayer(chatId);
        var currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        return treasureService.collectAllTreasure(chatId, player, currentRoom);
    }

    private boolean handleCollectingTreasureGold(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        treasureService.collectTreasureGold(chatId, player, currentRoom);
        return false;
    }

    private boolean handleMovingToRoom(Long chatId, CallbackType callBackData) {
        var player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Unable to find room with id {}", player.getCurrentRoomId());
            return false;
        }
        val newDirection = getDirectionSwitchByCallBackData(player.getDirection(), callBackData);
        val errorMessage = getErrorMessageByCallBackData(callBackData);
        if (!currentRoom.getAdjacentRooms().containsKey(newDirection) || !currentRoom.getAdjacentRooms().get((newDirection))) {
            log.error(errorMessage);
            return false;
        }
        val nextRoom = levelService.getRoomByChatIdAndCoordinates(chatId, getNextPointInDirection(currentRoom.getPoint(), newDirection));
        if (nextRoom == null) {
            log.error(errorMessage);
            return false;
        }
        log.debug("Moving to {} door: {}", callBackData.toString().toLowerCase(), nextRoom.getPoint());
        return levelService.moveToRoom(chatId, player, nextRoom, newDirection);
    }

    private boolean handleShrineRefill(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val level = levelService.getLevel(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (Objects.isNull(currentRoom)) {
            log.error("Unable to find room by Id:, {}", player.getCurrentRoomId());
            return false;
        }
        return levelService.shrineRefill(chatId, player, currentRoom, level);
    }

    private boolean handleAttack(Long chatId, CallbackType callBackData) {
        var player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        if (!getMonsterRoomTypes().contains(currentRoom.getRoomContent().getRoomType())) {
            log.error("No monster to attack!");
            return false;
        }
        return battleService.attack(chatId, player, currentRoom, callBackData);
    }

    private boolean handleOpenMerchantBuyMenu(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        return roomService.openMerchantBuyMenu(chatId, player, currentRoom);
    }

    private boolean handleOpenMerchantSellMenu(Long chatId) {
        val player = playerService.getPlayer(chatId);
        val currentRoom = roomService.getRoomByIdAndChatId(chatId, player.getCurrentRoomId());
        return roomService.openMerchantSellMenu(chatId, player, currentRoom);
    }

    private boolean handleSendingMapMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        return levelService.sendOrUpdateMapMessage(chatId, player);
    }

    private boolean handleSendingInventoryMessage(Long chatId) {
        val player = playerService.getPlayer(chatId);
        return inventoryService.sendOrUpdateInventoryMessage(chatId, player);
    }

    private Optional<CallbackType> getCallbackType(String callData) {
        return keyboardButtonProperties.getButtons().entrySet().stream()
                .filter(entry -> callData.equals(entry.getValue().getCallback()))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
