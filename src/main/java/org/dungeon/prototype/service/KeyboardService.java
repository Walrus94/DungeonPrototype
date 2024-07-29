package org.dungeon.prototype.service;

import lombok.val;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.Treasure;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.KeyboardButtonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.dungeon.prototype.properties.CallbackType.ATTACK;
import static org.dungeon.prototype.properties.CallbackType.BACK;
import static org.dungeon.prototype.properties.CallbackType.BOOTS;
import static org.dungeon.prototype.properties.CallbackType.COLLECT_ALL;
import static org.dungeon.prototype.properties.CallbackType.CONTINUE_GAME;
import static org.dungeon.prototype.properties.CallbackType.FORWARD;
import static org.dungeon.prototype.properties.CallbackType.GLOVES;
import static org.dungeon.prototype.properties.CallbackType.HEAD;
import static org.dungeon.prototype.properties.CallbackType.INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.ITEM_COLLECTED;
import static org.dungeon.prototype.properties.CallbackType.ITEM_INVENTORY;
import static org.dungeon.prototype.properties.CallbackType.ITEM_MERCHANT;
import static org.dungeon.prototype.properties.CallbackType.LEFT;
import static org.dungeon.prototype.properties.CallbackType.LEFT_HAND;
import static org.dungeon.prototype.properties.CallbackType.MAP;
import static org.dungeon.prototype.properties.CallbackType.MENU;
import static org.dungeon.prototype.properties.CallbackType.MENU_BACK;
import static org.dungeon.prototype.properties.CallbackType.MERCHANT;
import static org.dungeon.prototype.properties.CallbackType.NEXT_LEVEL;
import static org.dungeon.prototype.properties.CallbackType.RIGHT;
import static org.dungeon.prototype.properties.CallbackType.RIGHT_HAND;
import static org.dungeon.prototype.properties.CallbackType.SECONDARY_ATTACK;
import static org.dungeon.prototype.properties.CallbackType.SHRINE;
import static org.dungeon.prototype.properties.CallbackType.START_GAME;
import static org.dungeon.prototype.properties.CallbackType.TREASURE_GOLD_COLLECTED;
import static org.dungeon.prototype.properties.CallbackType.TREASURE_OPEN;
import static org.dungeon.prototype.properties.CallbackType.VEST;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.turnLeft;
import static org.dungeon.prototype.util.LevelUtil.turnRight;
import static org.dungeon.prototype.util.RoomGenerationUtils.getMonsterRoomTypes;

@Service
public class KeyboardService {

    @Autowired
    private KeyboardButtonProperties keyboardButtonProperties;

    public InlineKeyboardMarkup getStartInlineKeyboardMarkup(boolean hasSavedGame) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        hasSavedGame ? getButton(CONTINUE_GAME) : getButton(START_GAME)
                )))
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
        if (adjacentRooms.containsKey(turnLeft(direction)) && adjacentRooms.get(turnLeft(direction)) && !getMonsterRoomTypes().contains(roomContent.getRoomType())) {
            row1.add(getButton(LEFT));
        }
        if (adjacentRooms.containsKey(direction) && adjacentRooms.get(direction) && !getMonsterRoomTypes().contains(roomContent.getRoomType())) {
            row1.add(getButton(FORWARD));
        }
        if (adjacentRooms.containsKey(turnRight(direction)) && adjacentRooms.get(turnRight(direction)) && !getMonsterRoomTypes().contains(roomContent.getRoomType())) {
            row1.add(getButton(RIGHT));
        }
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(getButton(MENU));
        switch (roomContent.getRoomType()) {
            case TREASURE -> row2.add(getButton(TREASURE_OPEN));
            case WEREWOLF, SWAMP_BEAST, VAMPIRE, DRAGON, ZOMBIE -> {
                val weaponSet = player.getInventory().getWeaponSet();
                if (Objects.nonNull(weaponSet.getPrimaryWeapon())) {
                    row2.add(getButton(ATTACK));
                }
                if (Objects.nonNull(weaponSet.getSecondaryWeapon())) {
                    row2.add(getButton(SECONDARY_ATTACK));
                }
            }
            case HEALTH_SHRINE, MANA_SHRINE -> row2.add(getButton(SHRINE));
            case MERCHANT -> row2.add(getButton(MERCHANT));
        }
        if (adjacentRooms.containsKey(getOppositeDirection(direction)) && adjacentRooms.get(getOppositeDirection(direction))
                && !getMonsterRoomTypes().contains(roomContent.getRoomType())) {
            row2.add(getButton(BACK));
        }
        inlineKeyboard.setKeyboard(List.of(row1, row2));
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup getMenuInlineKeyboardMarkup() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getButton(INVENTORY));
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

    public InlineKeyboardMarkup getInventoryReplyMarkup(Player player) {
        val armor = player.getInventory().getArmorSet();
        val weapons = player.getInventory().getWeaponSet();
        val inventory = player.getInventory();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        if (Objects.nonNull(armor.getHelmet())) {
            InlineKeyboardButton button = getEquippedItemsListButton(armor.getHelmet(), HEAD);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            buttons.add(row);
        }
        if (Objects.nonNull(armor.getVest())) {
            InlineKeyboardButton button = getEquippedItemsListButton(armor.getVest(), VEST);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            buttons.add(row);
        }
        if (Objects.nonNull(armor.getGloves())) {
            InlineKeyboardButton button = getEquippedItemsListButton(armor.getGloves(), GLOVES);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            buttons.add(row);
        }
        if (Objects.nonNull(armor.getBoots())) {
            InlineKeyboardButton button = getEquippedItemsListButton(armor.getBoots(), BOOTS);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            buttons.add(row);
        }
        if (Objects.nonNull(weapons.getPrimaryWeapon())) {
            InlineKeyboardButton button = getEquippedItemsListButton(weapons.getPrimaryWeapon(), RIGHT_HAND);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            buttons.add(row);
        }
        if (Objects.nonNull(weapons.getSecondaryWeapon())) {
            InlineKeyboardButton button = getEquippedItemsListButton(weapons.getSecondaryWeapon(), LEFT_HAND);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            buttons.add(row);
        }
        if (Objects.nonNull(inventory) && !inventory.getItems().isEmpty()) {
            inventory.getItems().forEach(item -> {
                InlineKeyboardButton button = getItemListButton(item, ITEM_INVENTORY);
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                buttons.add(row);
            });
        }
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(getButton(MAP));
        row.add(getButton(MENU_BACK));
        buttons.add(row);
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getMerchantReplyMarkup(Set<Item> items) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        items.forEach(item -> {
            InlineKeyboardButton button = getMerchantSellListButton(item);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            buttons.add(row);
        });
        List<InlineKeyboardButton> row = new ArrayList<>();
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
                .text(keyboardButtonProperties.getButtons().get(type).getName().replace(keyboardButtonProperties.getTemplateReplacement(), value))
                .callbackData(keyboardButtonProperties.getButtons().get(type).getCallback())
                .build();
    }

    private InlineKeyboardButton getItemListButton(Item item, CallbackType listType) {
        return InlineKeyboardButton.builder()
                .text(keyboardButtonProperties.getButtons().get(listType).getName().replace(keyboardButtonProperties.getTemplateReplacement(), item.getName()))
                .callbackData(keyboardButtonProperties.getButtons().get(listType).getCallback().replace(keyboardButtonProperties.getTemplateReplacement(), item.getId()))
                .build();
    }

    private InlineKeyboardButton getEquippedItemsListButton(Item item, CallbackType equippedType) {
        val keyboardButtonAttributes = keyboardButtonProperties.getButtons().get(equippedType);
        return InlineKeyboardButton.builder()
                .text(keyboardButtonAttributes.getName().replace(keyboardButtonProperties.getTemplateReplacement(), keyboardButtonAttributes.getName() + ": " + item.getName()))
                .callbackData(keyboardButtonAttributes.getCallback().replace(keyboardButtonProperties.getTemplateReplacement(), item.getId()))
                .build();
    }

    private InlineKeyboardButton getMerchantSellListButton(Item item) {
        return InlineKeyboardButton.builder()
                .text(item.getName() + " : " + item.getSellingPrice())
                .callbackData(keyboardButtonProperties.getButtons().get(ITEM_MERCHANT).getCallback().replace(keyboardButtonProperties.getTemplateReplacement(), item.getId()))
                .build();
    }

    public CallbackType getCallbackType(String callData) {
        return keyboardButtonProperties.getButtons().entrySet().stream()
                .filter(entry -> callData.equals(entry.getValue().getCallback()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(InvalidParameterException::new);
    }
}
