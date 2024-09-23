package org.dungeon.prototype.exception;

public class CallbackException extends DungeonPrototypeException {
    public CallbackException(String callbackQueryId, String message) {
        super(String.format("Unable to answer callback query %s: %s", callbackQueryId, message));
    }
}
