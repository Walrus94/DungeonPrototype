package org.dungeon.prototype.exception;

import org.dungeon.prototype.properties.CallbackType;

public class ItemGenerationException extends PlayerException {
    public ItemGenerationException(Long chatId, String message, CallbackType buttonData) {
        super(String.format("Error on generating items: %s", message), chatId, buttonData);
    }
}
