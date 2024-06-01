package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;

public class NormalRoom implements RoomContent {
    @Override
    public Integer getRoomContentWeight() {
        return 0;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.NORMAL;
    }
}
