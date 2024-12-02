package org.dungeon.prototype.exception;

import lombok.Getter;

public class ChatException extends DungeonPrototypeException {
    @Getter
    private final long chatId;

    public ChatException(String message, long chatId) {
        super(message);
        this.chatId = chatId;
    }
}
