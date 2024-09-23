package org.dungeon.prototype.exception;

import org.dungeon.prototype.bot.ChatState;
import org.dungeon.prototype.properties.CallbackType;

public class ChatStateUpdateException extends PlayerException {
    public ChatStateUpdateException(Long chatId, ChatState from, ChatState to) {
        super(String.format("Unable to change state from %s to %s", from, to), chatId, CallbackType.DEFAULT_ERROR_RETURN);
    }
}
