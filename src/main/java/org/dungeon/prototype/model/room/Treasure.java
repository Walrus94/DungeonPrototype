package org.dungeon.prototype.model.room;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Treasure implements RoomContent {
    private Integer reward;

    @Override
    public Integer getRoomContentWeight() {
        return reward;
    }

    @Override
    public Room.Type getRoomType() {
        return Room.Type.TREASURE;
    }
}
