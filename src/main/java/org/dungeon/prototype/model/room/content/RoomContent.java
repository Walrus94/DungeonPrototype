package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomType;

public interface RoomContent {
    String getId();
    Integer getRoomContentWeight();
    RoomType getRoomType();
}
