package org.dungeon.prototype.exception;

import lombok.Getter;
import org.dungeon.prototype.properties.CallbackType;

public class PlayerException extends DungeonPrototypeException {
    @Getter
    private final long chatId;
    @Getter
    private final CallbackType button;

    public PlayerException(String message, Long chatId, CallbackType button) {
        super(message);
        this.chatId = chatId;
        this.button = button;
    }
}
