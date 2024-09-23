package org.dungeon.prototype.exception;

import lombok.Getter;
import org.dungeon.prototype.properties.CallbackType;

public class PlayerException extends DungeonPrototypeException {
    @Getter
    private final Long chatId;
    @Getter
    private final CallbackType buttonData;

    public PlayerException(String message, Long chatId, CallbackType buttonData) {
        super(message);
        this.chatId = chatId;
        this.buttonData = buttonData;
    }
}
