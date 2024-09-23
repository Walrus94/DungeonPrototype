package org.dungeon.prototype.model.room.content;

import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

public interface RoomContent {
    String getId();
    Weight getRoomContentWeight();
    RoomType getRoomType();
}
