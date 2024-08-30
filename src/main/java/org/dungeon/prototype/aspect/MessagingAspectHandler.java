package org.dungeon.prototype.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.dungeon.prototype.bot.DungeonBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Slf4j
@Aspect
@Component
public class MessagingAspectHandler {
    @Autowired
    DungeonBot dungeonBot;

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.AnswerCallback)", returning = "result")
    public void answerCallback(JoinPoint joinPoint, boolean result) {
        if (result) {
            handleCallbackAnswer(joinPoint);
        }
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.PhotoMessageSending)", returning = "message")
    public void sendMessage(JoinPoint joinPoint, SendPhoto message) {
        handleSendingPhotoMessage(joinPoint, message);
    }

    @AfterReturning(value = "@annotation(org.dungeon.prototype.annotations.aspect.MessageSending)", returning = "message")
    public void sendMessage(JoinPoint joinPoint, SendMessage message) {
        handleSendingMessage(joinPoint, message);
    }

    private void handleCallbackAnswer(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long) {
            if (args.length > 1 && args[1] instanceof CallbackQuery callbackQuery) {
                dungeonBot.answerCallbackQuery(callbackQuery.getId());
            }
        }
    }

    private void handleSendingPhotoMessage(JoinPoint joinPoint, SendPhoto message) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            dungeonBot.sendMessage(chatId, message);
        }
    }

    private void handleSendingMessage(JoinPoint joinPoint, SendMessage message) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            dungeonBot.sendMessage(chatId, message);
        }
    }
}
