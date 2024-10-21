package org.dungeon.prototype.exception;

import org.dungeon.prototype.bot.state.ChatState;
import org.dungeon.prototype.properties.CallbackType;

import java.util.Arrays;

public class ChatStateUpdateException extends PlayerException {
    public ChatStateUpdateException(Long chatId, ChatState to, ChatState... from) {
        super(String.format("Unable to change state from %s to %s", Arrays.toString(from), to), chatId, CallbackType.CONTINUE_GAME);
    }
}
