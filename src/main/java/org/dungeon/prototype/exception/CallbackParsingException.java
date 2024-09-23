package org.dungeon.prototype.exception;

public class CallbackParsingException extends DungeonPrototypeException {
    public CallbackParsingException(String callData) {
        super(String.format("Unable to parse %s", callData));
    }
}
