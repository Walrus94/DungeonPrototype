package org.dungeon.prototype.bot;

import lombok.val;
import org.dungeon.prototype.annotations.aspect.AnswerCallback;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.KeyboardButtonProperties;
import org.dungeon.prototype.service.BattleService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.dungeon.prototype.service.level.LevelService;
import org.dungeon.prototype.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Map;
import java.util.Optional;

import static org.dungeon.prototype.properties.CallbackType.ATTACK;
import static org.dungeon.prototype.properties.CallbackType.BOOTS;
import static org.dungeon.prototype.properties.CallbackType.GLOVES;
import static org.dungeon.prototype.properties.CallbackType.HEAD;
import static org.dungeon.prototype.properties.CallbackType.INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.LEFT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_MENU;
import static org.dungeon.prototype.properties.CallbackType.RIGHT_HAND;
import static org.dungeon.prototype.properties.CallbackType.VEST;

@Component
public class CallbackRouter {
    @Autowired
    PlayerService playerService;
    @Autowired
    LevelService levelService;
    @Autowired
    RoomService roomService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    BattleService battleService;
    @Autowired
    KeyboardButtonProperties keyboardButtonProperties;

    @AnswerCallback
    public boolean handleCallbackQuery(Long chatId, CallbackQuery callbackQuery) {
        val callData = callbackQuery.getData();
        val callBackData = getCallbackType(callData)
                .orElse(CallbackType.DEFAULT);

        return switch (callBackData) {
            case START_GAME ->
                    levelService.startNewGame(chatId);
            case CONTINUE_GAME ->
                    levelService.continueGame(chatId);
            case NEXT_LEVEL ->
                    levelService.nextLevel(chatId);
            case LEFT, RIGHT, FORWARD, BACK ->
                    levelService.moveToRoom(chatId, callBackData);
            case ATTACK, SECONDARY_ATTACK ->
                    battleService.attack(chatId, callBackData.equals(ATTACK));
            case TREASURE_OPEN ->
                    roomService.openTreasure(chatId);
            case TREASURE_GOLD_COLLECTED ->
                    roomService.collectTreasureGold(chatId);
            case SHRINE ->
                    levelService.shrineRefill(chatId);
            case MERCHANT_BUY_MENU, MERCHANT_BUY_MENU_BACK ->
                    roomService.openMerchantBuyMenu(chatId);
            case MERCHANT_SELL_MENU, MERCHANT_SELL_MENU_BACK ->
                    roomService.openMerchantSellMenu(chatId);
            case MAP ->
                    !levelService.sendOrUpdateMapMessage(chatId).isEmpty();
            case INVENTORY ->
                    inventoryService.sendOrUpdateInventoryMessage(chatId).isOk();
            case MENU_BACK ->
                    levelService.sendOrUpdateRoomMessage(chatId);
            case COLLECT_ALL ->
                    inventoryService.collectAllTreasure(chatId);
            case RESTORE_ARMOR -> playerService.restoreArmor(chatId);
            case SHARPEN_WEAPON -> true;
            default -> {
                if (callData.startsWith("btn_treasure_item_collected_")) {
                    val itemId = callData.replaceFirst("^" + "btn_treasure_item_collected_", "");
                    yield inventoryService.collectTreasureItem(chatId, itemId);
                }
                if (callData.startsWith("btn_inventory_display_")) {
                    if (callData.startsWith("btn_inventory_display_item_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_item_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.empty()).isOk();
                    }

                    if (callData.startsWith("btn_inventory_display_head_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_head_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(HEAD)).isOk();
                    }
                    if (callData.startsWith("btn_inventory_display_vest_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_vest_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(VEST)).isOk();
                    }
                    if (callData.startsWith("btn_inventory_display_gloves_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_gloves_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(GLOVES)).isOk();
                    }
                    if (callData.startsWith("btn_inventory_display_boots_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_boots_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(BOOTS)).isOk();
                    }
                    if (callData.startsWith("btn_inventory_display_primary_weapon_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_primary_weapon_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(RIGHT_HAND)).isOk();
                    }
                    if (callData.startsWith("btn_inventory_display_secondary_weapon_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_display_secondary_weapon_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, INVENTORY, Optional.of(LEFT_HAND)).isOk();
                    }
                }
                if (callData.startsWith("btn_inventory_item_")) {
                    if (callData.startsWith("btn_inventory_item_equip_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_item_equip_", "");
                        yield inventoryService.equipItem(chatId, itemId).isOk();
                    }
                    if (callData.startsWith("btn_inventory_item_un_equip_")) {
                        val itemId = callData.replaceFirst("^" + "btn_inventory_item_un_equip_", "");
                        yield inventoryService.unEquipItem(chatId, itemId).isOk();
                    }
                }
                if (callData.startsWith("btn_merchant_")) {

                    if (callData.startsWith("btn_merchant_list_item_sell_")) {
                        val itemId = callData.replaceFirst("^" + "btn_merchant_list_item_sell_", "");
                        yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.empty()).isOk();
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
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.empty()).isOk();
                        }
                        if (callData.startsWith("btn_merchant_display_head_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_head_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(HEAD)).isOk();
                        }
                        if (callData.startsWith("btn_merchant_display_vest_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_vest_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(VEST)).isOk();
                        }
                        if (callData.startsWith("btn_merchant_display_gloves_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_gloves_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(GLOVES)).isOk();
                        }
                        if (callData.startsWith("btn_merchant_display_boots_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_boots_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(BOOTS)).isOk();
                        }
                        if (callData.startsWith("btn_merchant_display_primary_weapon_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_primary_weapon_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(RIGHT_HAND)).isOk();
                        }
                        if (callData.startsWith("btn_merchant_display_secondary_weapon_")) {
                            val itemId = callData.replaceFirst("^" + "btn_merchant_display_secondary_weapon_", "");
                            yield inventoryService.openInventoryItemInfo(chatId, itemId, MERCHANT_SELL_MENU, Optional.of(LEFT_HAND)).isOk();
                        }
                    }
                }
                if (callData.startsWith("btn_player_attribute_upgrade_")) {
                    val playerAttribute = PlayerAttribute.fromValue(callData.replaceFirst("^" + "btn_player_attribute_upgrade_", ""));
                    yield playerService.upgradePlayerAttribute(chatId, playerAttribute);
                }
                yield false;
            }
        };
    }

    public Optional<CallbackType> getCallbackType(String callData) {
        return keyboardButtonProperties.getButtons().entrySet().stream()
                .filter(entry -> callData.equals(entry.getValue().getCallback()))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
