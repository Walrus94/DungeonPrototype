package org.dungeon.prototype.model.room.content;

import lombok.Data;

@Data
public abstract class NoContentRoom implements RoomContent {
    protected String id;
    public Integer getRoomContentWeight() {
        return 0;
    }
}
