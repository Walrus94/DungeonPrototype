package org.dungeon.prototype.bot.state;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.dungeon.prototype.bot.state.ChatState.PRE_GAME_MENU;


@Data
public class ChatContext {
    private AtomicInteger lastMessageId;
    private ChatState chatState = PRE_GAME_MENU;
    private AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());
}
