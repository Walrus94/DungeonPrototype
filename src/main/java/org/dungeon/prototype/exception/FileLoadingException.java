package org.dungeon.prototype.exception;

import org.dungeon.prototype.properties.CallbackType;

public class FileLoadingException extends PlayerException {
    public FileLoadingException(Long chatId, String message) {
        super(String.format("Error loading file: %s", message), chatId, CallbackType.DEFAULT_ERROR_RETURN);
    }
}
