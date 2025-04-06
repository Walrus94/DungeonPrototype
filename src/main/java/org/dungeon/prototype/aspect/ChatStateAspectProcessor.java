package org.dungeon.prototype.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.dungeon.prototype.annotations.aspect.ChatStateUpdate;
import org.dungeon.prototype.bot.state.ChatState;
import org.dungeon.prototype.service.state.ChatStateService;
import org.dungeon.prototype.exception.ChatStateUpdateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class ChatStateAspectProcessor {
    @Autowired
    private ChatStateService chatStateService;

    @Before(value = "@annotation(org.dungeon.prototype.annotations.aspect.InitializeChatContext)")
    public void initializeChatContext(JoinPoint joinPoint) {
        handleChatContextInitialization(joinPoint);
    }

    @Around(value = "@annotation(org.dungeon.prototype.annotations.aspect.ChatStateUpdate)")
    public Object updateChatState(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Get the annotation from the method
        ChatStateUpdate chatStateUpdate = method.getAnnotation(ChatStateUpdate.class);
        if (chatStateUpdate != null) {
            // Access the annotation values
            ChatState[] from = chatStateUpdate.from();
            ChatState to = chatStateUpdate.to();

            return handleChatStateUpdate(joinPoint, from, to);
        }
        return null;
    }

    @After(value = "@annotation(org.dungeon.prototype.annotations.aspect.ClearChatContext)")
    public void clearChatContext(JoinPoint joinPoint) {
        handleClearChatContext(joinPoint);
    }

    private void handleChatContextInitialization(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            chatStateService.initializeChatContext(chatId);
        }
    }

    private Object handleChatStateUpdate(ProceedingJoinPoint joinPoint, ChatState[] from, ChatState to) throws ChatStateUpdateException {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            try {
                chatStateService.updateChatState(chatId, to, from);
                // Proceed with the original method call
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (e instanceof ChatStateUpdateException) {
                    throw (ChatStateUpdateException) e;
                } else {
                    throw new ChatStateUpdateException(chatId, to, from);
                }
            }
        }
        return null;
    }

    private void handleClearChatContext(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long chatId) {
            chatStateService.clearChatContext(chatId);
        }
    }
}
