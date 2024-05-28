package org.dungeon.prototype.model.room;

public class Shrine implements RoomContent {
    @Override
    public Integer getRoomContentWeight() {
        return 100;
    }

    @Override
    public Room.Type getRoomType() {
        return Room.Type.SHRINE;
    }
}
