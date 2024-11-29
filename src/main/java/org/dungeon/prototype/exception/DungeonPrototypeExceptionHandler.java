package org.dungeon.prototype.exception;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.service.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class DungeonPrototypeExceptionHandler {
    @Autowired
    private MessageService messageService;

    @ExceptionHandler(CallbackException.class)
    public void handleCallbackException(CallbackException e) {
        log.error(e.getMessage());
    }

    @ExceptionHandler(PlayerException.class)
    public void handleException(PlayerException e) {
        log.error("Exception occurred: {}", e.getMessage());
        messageService.sendErrorMessage(e);
    }

    @ExceptionHandler(ChatException.class)
    public void handleException(ChatException e) {
        log.error("Exception occurred: {}", e.getMessage());
        messageService.sendErrorMessage(e);
    }
}
