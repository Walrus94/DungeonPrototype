package org.dungeon.prototype.exception;

import org.dungeon.prototype.properties.CallbackType;

public class DeleteMessageException extends PlayerException {
    public DeleteMessageException(Long chatId, Integer messageId, CallbackType buttonData) {
        super(String.format("Unable to delete message id:%s", messageId), chatId, buttonData);

    }
    public DeleteMessageException(Long chatId, Integer messageId, String message, CallbackType buttonData) {
        super(String.format("Unable to delete message id:%s. %s", messageId, message), chatId, buttonData);
    }
}
