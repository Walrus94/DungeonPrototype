package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import org.dungeon.prototype.model.Room;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.turnLeft;
import static org.dungeon.prototype.util.LevelUtil.turnRight;

@UtilityClass
public class KeyboardUtil {
    public static InlineKeyboardMarkup getStartInlineKeyboardMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                                .text("Start Game!")
                                .callbackData("start_game")
                                .build()
                ))).build();
    }
    public static InlineKeyboardMarkup getRoomInlineKeyboardMarkup(Room room, LevelUtil.Direction direction) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        if (Room.Type.END.equals(room.getType())) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Next Level")
                    .callbackData("btn_next_level")
                    .build());
        }
        if (room.getAdjacentRooms().get(turnLeft(direction)).isPresent()) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Left")
                    .callbackData("btn_left")
                    .build());
        }
        if (room.getAdjacentRooms().get(direction).isPresent()) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Middle")
                    .callbackData("btn_middle")
                    .build());
        }
        if (room.getAdjacentRooms().get(turnRight(direction)).isPresent()) {
            row1.add(InlineKeyboardButton.builder()
                    .text("Right")
                    .callbackData("btn_right")
                    .build());
        }
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("Map")
                .callbackData("btn_map")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("Action")
                .callbackData("btn_action")
                .build());
        if (room.getAdjacentRooms().get(getOppositeDirection(direction)).isPresent()) {
            row2.add(InlineKeyboardButton.builder()
                    .text("Turn Back")
                    .callbackData("btn_turn_back")
                    .build());
        }
        inlineKeyboard.setKeyboard(List.of(row1, row2));
        return inlineKeyboard;
    }
}
