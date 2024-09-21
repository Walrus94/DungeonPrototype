package org.dungeon.prototype.model.room.content;

import lombok.Data;
import org.dungeon.prototype.model.weight.Weight;

@Data
public abstract class NoContentRoom implements RoomContent {
    protected String id;
    public Weight getRoomContentWeight() {
        return new Weight();
    }
}
