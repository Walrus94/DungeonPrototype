package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomType;

public class HealthShrine extends Shrine{
    @Override
    public Integer getRoomContentWeight() {
        return 500;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.HEALTH_SHRINE;
    }
}
