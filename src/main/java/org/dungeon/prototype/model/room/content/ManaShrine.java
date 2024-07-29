package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomType;

public class ManaShrine extends Shrine {
    @Override
    public Integer getRoomContentWeight() {
        return 300;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.MANA_SHRINE;
    }
}
