package org.dungeon.prototype.model.room;

public class NormalRoom implements RoomContent {
    @Override
    public Integer getRoomContentWeight() {
        return 0;
    }

    @Override
    public Room.Type getRoomType() {
        return Room.Type.NORMAL;
    }
}
