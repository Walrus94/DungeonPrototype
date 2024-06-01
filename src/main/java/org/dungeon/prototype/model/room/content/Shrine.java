package org.dungeon.prototype.model.room.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;

@Data
@AllArgsConstructor
public class Shrine implements RoomContent {
    @Override
    public Integer getRoomContentWeight() {
        return 100;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SHRINE;
    }
}
