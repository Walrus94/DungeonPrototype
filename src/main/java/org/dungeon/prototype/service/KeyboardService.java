package org.dungeon.prototype.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.Anvil;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.KeyboardButtonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.dungeon.prototype.properties.CallbackType.ATTACK;
import static org.dungeon.prototype.properties.CallbackType.BACK;
import static org.dungeon.prototype.properties.CallbackType.BOOTS;
import static org.dungeon.prototype.properties.CallbackType.COLLECT_ALL;
import static org.dungeon.prototype.properties.CallbackType.CONTINUE_GAME;
import static org.dungeon.prototype.properties.CallbackType.DEFAULT;
import static org.dungeon.prototype.properties.CallbackType.FORWARD;
import static org.dungeon.prototype.properties.CallbackType.GLOVES;
import static org.dungeon.prototype.properties.CallbackType.HEAD;
import static org.dungeon.prototype.properties.CallbackType.INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.ITEM_COLLECTED;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY_BACK;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY_EQUIP;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY_UN_EQUIP;
import static org.dungeon.prototype.properties.CallbackType.LEFT;
import static org.dungeon.prototype.properties.CallbackType.LEFT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MAP;
import static org.dungeon.prototype.properties.CallbackType.MENU_BACK;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_BUY_MENU;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_BUY_MENU_BACK;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_BUY_PRICE;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_ITEM_BUY;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_BOOTS;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_GLOVES;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_HEAD;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_LEFT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_RIGHT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_DISPLAY_VEST;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_MENU;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_MENU_BACK;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT_SELL_PRICE;
import static org.dungeon.prototype.properties.CallbackType.NEXT_LEVEL;
import static org.dungeon.prototype.properties.CallbackType.PLAYER_ATTRIBUTE_UPGRADE;
import static org.dungeon.prototype.properties.CallbackType.PLAYER_STATS;
import static org.dungeon.prototype.properties.CallbackType.RESTORE_ARMOR;
import static org.dungeon.prototype.properties.CallbackType.RIGHT;
import static org.dungeon.prototype.properties.CallbackType.RIGHT_HAND;
import static org.dungeon.prototype.properties.CallbackType.SECONDARY_ATTACK;
import static org.dungeon.prototype.properties.CallbackType.SHARPEN_WEAPON;
import static org.dungeon.prototype.properties.CallbackType.SHRINE;
import static org.dungeon.prototype.properties.CallbackType.START_GAME;
import static org.dungeon.prototype.properties.CallbackType.TREASURE_GOLD_COLLECTED;
import static org.dungeon.prototype.properties.CallbackType.TREASURE_OPEN;
import static org.dungeon.prototype.properties.CallbackType.VEST;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.turnLeft;
import static org.dungeon.prototype.util.LevelUtil.turnRight;
import static org.dungeon.prototype.util.MessageUtil.formatItemType;
import static org.dungeon.prototype.util.RoomGenerationUtils.getMonsterRoomTypes;

@Service
@RequiredArgsConstructor
public class KeyboardService {

    @Autowired
    private KeyboardButtonProperties keyboardButtonProperties;

    private String templatePlaceholder;

    @PostConstruct
    public void init() {
        this.templatePlaceholder = keyboardButtonProperties.getTemplatePlaceholder();
    }

    public InlineKeyboardMarkup getStartInlineKeyboardMarkup(boolean hasSavedGame) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        hasSavedGame ? getButton(CONTINUE_GAME) : getButton(START_GAME)
                )))
                .build();
    }

    public InlineKeyboardMarkup getDeathMessageInlineKeyboardMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(getButton(START_GAME)))
                .build();
    }

    public InlineKeyboardMarkup getRoomInlineKeyboardMarkup(Room room, Player player) {
        val roomContent = room.getRoomContent();
        val adjacentRooms = room.getAdjacentRooms();
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        if (RoomType.END.equals(roomContent.getRoomType())) {
            row1.add(getButton(NEXT_LEVEL));
        }
        val direction = player.getDirection();
        val isMonsterRoom = getMonsterRoomTypes().contains(roomContent.getRoomType());
        if (adjacentRooms.containsKey(turnLeft(direction)) && adjacentRooms.get(turnLeft(direction)) && !isMonsterRoom) {
            row1.add(getButton(LEFT));
        }
        if (adjacentRooms.containsKey(direction) && adjacentRooms.get(direction) && !isMonsterRoom) {
            row1.add(getButton(FORWARD));
        }
        if (adjacentRooms.containsKey(turnRight(direction)) && adjacentRooms.get(turnRight(direction)) && !isMonsterRoom) {
            row1.add(getButton(RIGHT));
        }
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        switch (roomContent.getRoomType()) {
            case TREASURE -> row2.add(getButton(TREASURE_OPEN));
            case WEREWOLF, SWAMP_BEAST, VAMPIRE, DRAGON, ZOMBIE -> {
                val weaponSet = player.getInventory().getWeaponSet();
                row1.add(getButton(ATTACK, player.getPrimaryAttack().toString()));
                if (Objects.nonNull(weaponSet.getSecondaryWeapon())) {
                    row1.add(getButton(SECONDARY_ATTACK, player.getSecondaryAttack().toString()));
                }
            }
            case HEALTH_SHRINE, MANA_SHRINE -> row2.add(getButton(SHRINE));
            case MERCHANT -> {
                row2.add(getButton(MERCHANT_SELL_MENU));
                row2.add(getButton(MERCHANT_BUY_MENU));
            }

            case ANVIL -> {
                val anvil = (Anvil) room.getRoomContent();
                if (!anvil.isArmorRestored()) {
                    row2.add(getButton(RESTORE_ARMOR));
                }
                row2.add(getButton(SHARPEN_WEAPON));
            }
        }
        row2.add(getButton(MAP));
        if (!isMonsterRoom) {
            row2.add(getButton(INVENTORY));
        }
        row2.add(getButton(PLAYER_STATS));
        if (adjacentRooms.containsKey(getOppositeDirection(direction)) && adjacentRooms.get(getOppositeDirection(direction))
                && !isMonsterRoom) {
            row2.add(getButton(BACK));
        }
        inlineKeyboard.setKeyboard(List.of(row1, row2));
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup getMapInlineKeyboardMarkup() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getButton(INVENTORY));
        row.add(getButton(PLAYER_STATS));
        row.add(getButton(MENU_BACK));

        return new InlineKeyboardMarkup(List.of(row));
    }

    public InlineKeyboardMarkup getTreasureContentReplyMarkup(Treasure treasure) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        if (treasure.getGold() > 0) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(getButton(TREASURE_GOLD_COLLECTED, treasure.getGold().toString()));
            buttons.add(row);
        }
        treasure.getItems().forEach(item -> {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(getItemListButton(item, ITEM_COLLECTED));
            buttons.add(row);
        });
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getButton(COLLECT_ALL));
        row.add(getButton(MENU_BACK));
        buttons.add(row);

        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getInventoryReplyMarkup(Inventory inventory, CallbackType unEquippedItem, CallbackType equippedItemAction, CallbackType unEquippedItemAction, List<CallbackType> additionalMenus) {
        val armor = inventory.getArmorSet();
        val weapons = inventory.getWeaponSet();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        if (Objects.nonNull(armor.getHelmet())) {
            List<InlineKeyboardButton> row = getInventoryItemsListButtonRow(armor.getHelmet(), HEAD, equippedItemAction);
            buttons.add(row);
        }
        if (Objects.nonNull(armor.getVest())) {
            List<InlineKeyboardButton> row = getInventoryItemsListButtonRow(armor.getVest(), VEST, equippedItemAction);
            buttons.add(row);
        }
        if (Objects.nonNull(armor.getGloves())) {
            List<InlineKeyboardButton> row = getInventoryItemsListButtonRow(armor.getGloves(), GLOVES, equippedItemAction);
            buttons.add(row);
        }
        if (Objects.nonNull(armor.getBoots())) {
            List<InlineKeyboardButton> row = getInventoryItemsListButtonRow(armor.getBoots(), BOOTS, equippedItemAction);
            buttons.add(row);
        }
        if (Objects.nonNull(weapons.getPrimaryWeapon())) {
            List<InlineKeyboardButton> row = getInventoryItemsListButtonRow(weapons.getPrimaryWeapon(), RIGHT_HAND, equippedItemAction);
            buttons.add(row);
        }
        if (Objects.nonNull(weapons.getSecondaryWeapon())) {
            List<InlineKeyboardButton> row = getInventoryItemsListButtonRow(weapons.getSecondaryWeapon(), LEFT_HAND, equippedItemAction);
            buttons.add(row);
        }
        if (!inventory.getItems().isEmpty()) {
            inventory.getItems().forEach(item -> {
                List<InlineKeyboardButton> row = getInventoryItemListRow(item, unEquippedItem, unEquippedItemAction);
                buttons.add(row);
            });
        }
        List<InlineKeyboardButton> row = new ArrayList<>();
        additionalMenus.forEach(additionalMenu ->
                row.add(getButton(additionalMenu)));
        row.add(getButton(MENU_BACK));
        buttons.add(row);
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getPlayerStatsReplyMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getButton(MAP));
        row.add(getButton(INVENTORY));
        row.add(getButton(MENU_BACK));
        buttons.add(row);
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getMerchantBuyListReplyMarkup(Set<Item> items) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        items.forEach(item -> {
            InlineKeyboardButton itemButton = getMerchantItemBuyListButton(item);
            InlineKeyboardButton buyButton = getMerchantBuyPriceButton(item);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(itemButton);
            row.add(buyButton);
            buttons.add(row);
        });
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getButton(MERCHANT_SELL_MENU));
        row.add(getButton(MENU_BACK));
        buttons.add(row);
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardButton getButton(CallbackType type) {
        return InlineKeyboardButton.builder()
                .text(keyboardButtonProperties.getButtons().get(type).getName())
                .callbackData(keyboardButtonProperties.getButtons().get(type).getCallback())
                .build();
    }

    private InlineKeyboardButton getButton(CallbackType type, String value) {
        return InlineKeyboardButton.builder()
                .text(keyboardButtonProperties.getButtons().get(type).getName().replace(templatePlaceholder, value))
                .callbackData(keyboardButtonProperties.getButtons().get(type).getCallback())
                .build();
    }

    private InlineKeyboardButton getItemListButton(Item item, CallbackType listType) {
        return InlineKeyboardButton.builder()
                .text(keyboardButtonProperties.getButtons().get(listType).getName().replace(templatePlaceholder, item.getName()))
                .callbackData(keyboardButtonProperties.getButtons().get(listType).getCallback().replace(templatePlaceholder, item.getId()))
                .build();
    }

    private List<InlineKeyboardButton> getInventoryItemListRow(Item item, CallbackType itemType, CallbackType itemAction) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        val itemButtonProperties = keyboardButtonProperties.getButtons().get(itemType);
        val actionButtonProperties = keyboardButtonProperties.getButtons().get(itemAction);
        val templateReplacement = templatePlaceholder;
        row.add(InlineKeyboardButton.builder()
                .text(itemButtonProperties.getName().replace(templateReplacement, item.getName()))
                .callbackData(itemButtonProperties.getCallback().replace(templateReplacement, item.getId()))
                .build());
        row.add(InlineKeyboardButton.builder()
                .text(actionButtonProperties.getName().replace(templatePlaceholder, item.getSellingPrice().toString()))
                .callbackData(actionButtonProperties.getCallback().replace(templateReplacement, item.getId()))
                .build());
        return row;
    }

    public InlineKeyboardMarkup getInventoryItemInfoReplyMarkup(Item item, CallbackType inventoryType) {
        KeyboardButtonProperties.KeyboardButtonAttributes actionButton;
        KeyboardButtonProperties.KeyboardButtonAttributes backButton;
        switch (inventoryType) {
            case INVENTORY ->
            {
                actionButton = keyboardButtonProperties.getButtons().get(ITEM_INVENTORY_EQUIP);
                backButton = keyboardButtonProperties.getButtons().get(ITEM_INVENTORY_BACK);

            }
            case MERCHANT_SELL_MENU ->
            {
                actionButton = keyboardButtonProperties.getButtons().get(MERCHANT_SELL_PRICE);
                backButton = keyboardButtonProperties.getButtons().get(MERCHANT_SELL_MENU_BACK);
            }
            default -> {
                actionButton = keyboardButtonProperties.getButtons().get(DEFAULT);
                backButton = keyboardButtonProperties.getButtons().get(DEFAULT);
            }
        }
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text(actionButton.getName().replace(templatePlaceholder, item.getSellingPrice().toString()))
                .callbackData(actionButton.getCallback().replace(templatePlaceholder, item.getId()))
                .build());
        row.add(InlineKeyboardButton.builder()
                .text(backButton.getName())
                .callbackData(backButton.getCallback())
                .build());
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
    }

    public ReplyKeyboard getEquippedItemInfoReplyMarkup(CallbackType inventoryType, Integer sellPrice) {
        val backButtonType = switch (inventoryType) {
            case INVENTORY -> ITEM_INVENTORY_BACK;
            case MERCHANT_SELL_MENU ->  MERCHANT_SELL_MENU_BACK;
            default -> DEFAULT;
        };
        val actionButtonType = switch (inventoryType) {
            case INVENTORY -> ITEM_INVENTORY_UN_EQUIP;
            case MERCHANT_SELL_MENU -> MERCHANT_SELL_PRICE;
            default -> DEFAULT;
        };
        val actionButtonProperties = keyboardButtonProperties.getButtons().get(actionButtonType);
        val backButtonProperties = keyboardButtonProperties.getButtons().get(backButtonType);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text(actionButtonProperties.getName())
                .callbackData(actionButtonProperties.getCallback().replace(keyboardButtonProperties.getTemplatePlaceholder(), sellPrice.toString()))
                .build());
        row.add(InlineKeyboardButton.builder()
                .text(backButtonProperties.getName())
                .callbackData(backButtonProperties.getCallback())
                .build());
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
    }

    private List<InlineKeyboardButton> getInventoryItemsListButtonRow(Item item, CallbackType equippedType, CallbackType itemAction) {
        //TODO: get rid of this
        CallbackType buttonNameType;
        if (MERCHANT_SELL_PRICE.equals(itemAction)) {
            buttonNameType = switch (equippedType) {
                case HEAD -> MERCHANT_SELL_DISPLAY_HEAD;
                case VEST -> MERCHANT_SELL_DISPLAY_VEST;
                case GLOVES -> MERCHANT_SELL_DISPLAY_GLOVES;
                case BOOTS -> MERCHANT_SELL_DISPLAY_BOOTS;
                case LEFT_HAND -> MERCHANT_SELL_DISPLAY_LEFT_HAND;
                case RIGHT_HAND -> MERCHANT_SELL_DISPLAY_RIGHT_HAND;
                default -> equippedType;
            };
        } else {
            buttonNameType = equippedType;
        }
        val itemButtonAttributes = keyboardButtonProperties.getButtons().get(buttonNameType);
        val unEquipButtonAttributes = keyboardButtonProperties.getButtons().get(itemAction);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getListItemButton(item, equippedType, itemButtonAttributes));
        row.add(InlineKeyboardButton.builder()
                .text(unEquipButtonAttributes.getName().replace(templatePlaceholder, item.getSellingPrice().toString()))
                .callbackData(unEquipButtonAttributes.getCallback().replace(templatePlaceholder, item.getId()))
                .build());
        return row;
    }

    private InlineKeyboardButton getListItemButton(Item item, CallbackType equippedType, KeyboardButtonProperties.KeyboardButtonAttributes itemButtonAttributes) {
        return InlineKeyboardButton.builder()
                .text(itemButtonAttributes.getName().replace(templatePlaceholder, formatItemType(equippedType) + ": " + item.getName()))
                .callbackData(itemButtonAttributes.getCallback().replace(templatePlaceholder, item.getId()))
                .build();
    }

    public InlineKeyboardMarkup getMerchantBuyItemInfoReplyMarkup(Item item) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getMerchantBuyPriceButton(item));
        row.add(getButton(MERCHANT_BUY_MENU_BACK));
        buttons.add(row);
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getNewLevelUpgradeReplyMarkup(Player player) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        player.getAttributes().forEach((playerAttribute, value) -> {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(getPlayerAttributeButton(playerAttribute, value));
            buttons.add(row);
        });
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardButton getPlayerAttributeButton(PlayerAttribute playerAttribute, Integer value) {
        val buttonProperties = keyboardButtonProperties.getButtons().get(PLAYER_ATTRIBUTE_UPGRADE);
        return InlineKeyboardButton.builder()
                .text(playerAttribute.getValue() + ": " + value + " (+1)")
                .callbackData(buttonProperties.getCallback().replace(keyboardButtonProperties.getTemplatePlaceholder(), playerAttribute.getValue()))
                .build();
    }

    private InlineKeyboardButton getMerchantItemBuyListButton(Item item) {
        val buttonProperties = keyboardButtonProperties.getButtons().get(MERCHANT_ITEM_BUY);
        return InlineKeyboardButton.builder()
                .text(item.getName())
                .callbackData(buttonProperties.getCallback().replace(templatePlaceholder, item.getId()))
                .build();
    }

    private InlineKeyboardButton getMerchantBuyPriceButton(Item item) {
        val buttonProperties = keyboardButtonProperties.getButtons().get(MERCHANT_BUY_PRICE);
        return InlineKeyboardButton.builder()
                .text(buttonProperties.getName().replace(templatePlaceholder, item.getBuyingPrice().toString()))
                .callbackData(buttonProperties.getCallback().replace(templatePlaceholder, item.getId()))
                .build();
    }
}
