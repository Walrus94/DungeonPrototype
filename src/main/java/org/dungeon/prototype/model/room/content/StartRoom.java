package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomType;

public class StartRoom extends NoContentRoom {
    @Override
    public RoomType getRoomType() {
        return RoomType.START;
    }
}
