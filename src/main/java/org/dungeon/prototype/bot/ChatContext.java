package org.dungeon.prototype.bot;

import lombok.Data;

import static org.dungeon.prototype.bot.ChatState.ACTIVE;


@Data
public class ChatContext {
    private Integer lastMessageId;
    private ChatState chatState = ACTIVE;
    private long lastActiveTime = System.currentTimeMillis();
}
