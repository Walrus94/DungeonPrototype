package org.dungeon.prototype.exception;

import org.dungeon.prototype.properties.CallbackType;

public class SendMessageException extends PlayerException {
    public SendMessageException(String message, Long chatId, CallbackType buttonData) {
        super(String.format("Unable to send message: %s", message), chatId, buttonData);
    }

    public SendMessageException(Long chatId, CallbackType buttonData) {
        super("Unable to send message!", chatId, buttonData);
    }
}
