package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.turnLeft;
import static org.dungeon.prototype.util.LevelUtil.turnRight;

@UtilityClass
public class KeyboardUtil {
    public static InlineKeyboardMarkup getStartInlineKeyboardMarkup(boolean hasSavedGame) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        hasSavedGame ? InlineKeyboardButton.builder()
                                .text("Continue game")
                                .callbackData("continue_game")
                                .build() : InlineKeyboardButton.builder()
                                .text("Start Game!")
                                .callbackData("start_game")
                                .build()
                )))
                .build();
    }
    public static InlineKeyboardMarkup getRoomInlineKeyboardMarkup(Room room, Direction direction) {
        val roomContent = room.getRoomContent();
        val adjacentRooms = room.getAdjacentRooms();
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        if (RoomType.END.equals(roomContent.getRoomType())) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Next Level")
                    .callbackData("btn_next_level")
                    .build());
        }
        if (adjacentRooms.containsKey(turnLeft(direction)) && adjacentRooms.get(turnLeft(direction)) && !RoomType.MONSTER.equals(roomContent.getRoomType())) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Left")
                    .callbackData("btn_left")
                    .build());
        }
        if (adjacentRooms.containsKey(direction) && adjacentRooms.get(direction) && !RoomType.MONSTER.equals(roomContent.getRoomType())) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Forward")
                    .callbackData("btn_middle")
                    .build());
        }
        if (adjacentRooms.containsKey(turnRight(direction)) && adjacentRooms.get(turnRight(direction)) && !RoomType.MONSTER.equals(roomContent.getRoomType())) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Right")
                    .callbackData("btn_right")
                    .build());
        }
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("Menu")
                .callbackData("btn_menu")
                .build());
        switch (roomContent.getRoomType()) {
            case TREASURE -> row2.add(InlineKeyboardButton.builder()
                    .text("Collect")
                    .callbackData("btn_collect")
                    .build());
            case MONSTER -> row2.add(InlineKeyboardButton.builder()
                    .text("Attack!")
                    .callbackData("btn_attack")
                    .build());
            case HEALTH_SHRINE, MANA_SHRINE -> row2.add(InlineKeyboardButton.builder()
                    .text("Use")
                    .callbackData("btn_shrine_use")
                    .build());
            case MERCHANT -> row2.add(InlineKeyboardButton.builder()
                    .text("Buy and sell!")
                    .callbackData("btn_merchant")
                    .build());
        }
        if (adjacentRooms.containsKey(getOppositeDirection(direction)) && adjacentRooms.get(getOppositeDirection(direction))
                && !RoomType.MONSTER.equals(roomContent.getRoomType())) {
            row2.add(InlineKeyboardButton.builder()
                    .text("Turn Back")
                    .callbackData("btn_turn_back")
                    .build());
        }
        inlineKeyboard.setKeyboard(List.of(row1, row2));
        return inlineKeyboard;
    }

    public static InlineKeyboardMarkup getMenuInlineKeyboardMarkup() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("Inventory")
                .callbackData("btn_inventory")
                .build());
        row.add(InlineKeyboardButton.builder()
                .text("Back")
                .callbackData("btn_menu_back")
                .build());

        return new InlineKeyboardMarkup(List.of(row));
    }
}
