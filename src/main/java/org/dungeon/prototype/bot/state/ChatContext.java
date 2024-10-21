package org.dungeon.prototype.bot.state;

import lombok.Data;

import static org.dungeon.prototype.bot.state.ChatState.PRE_GAME_MENU;


@Data
public class ChatContext {
    private Integer lastMessageId;
    private ChatState chatState = PRE_GAME_MENU;
    private long lastActiveTime = System.currentTimeMillis();
}
