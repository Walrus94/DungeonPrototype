package org.dungeon.prototype.bot;

import lombok.Data;

import static org.dungeon.prototype.bot.State.ACTIVE;


@Data
public class ChatState {
    private Integer lastMessageId;
    private Boolean awaitingNickname = false;
    private State state = ACTIVE;
}
