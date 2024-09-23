package org.dungeon.prototype.exception;

import org.dungeon.prototype.properties.CallbackType;

public class RestrictedOperationException extends PlayerException {
    public RestrictedOperationException(Long chatId, String operation, String message, CallbackType buttonData) {
        super(String.format("Unable to %s: %s", operation, message), chatId, buttonData);
    }
}
