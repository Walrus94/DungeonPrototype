package org.dungeon.prototype.model.room.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;

@Data
@AllArgsConstructor
public class Treasure implements RoomContent {
    private Integer reward;

    @Override
    public Integer getRoomContentWeight() {
        return reward;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.TREASURE;
    }
}
