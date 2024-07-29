package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomType;

public class EmptyRoom extends NoContentRoom {

    public EmptyRoom(RoomType roomType) {
        this.roomType = roomType;
    }
    private final RoomType roomType;

    @Override
    public RoomType getRoomType() {
        return roomType;
    }
}
