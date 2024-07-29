package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomType;

public class NormalRoom extends NoContentRoom {
    @Override
    public RoomType getRoomType() {
        return RoomType.NORMAL;
    }
}
