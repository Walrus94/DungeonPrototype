package org.dungeon.prototype.annotations.aspect;

import org.dungeon.prototype.bot.ChatState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ChatStateUpdate {
    ChatState from();
    ChatState to();
}
