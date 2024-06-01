package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;

public class EmptyRoom implements RoomContent {

    public EmptyRoom(RoomType roomType) {
        this.roomType = roomType;
    }
    private final RoomType roomType;
    @Override
    public Integer getRoomContentWeight() {
        return 0;
    }

    @Override
    public RoomType getRoomType() {
        return roomType;
    }
}
