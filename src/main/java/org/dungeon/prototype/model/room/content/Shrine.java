package org.dungeon.prototype.model.room.content;

import lombok.Data;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.weight.Weight;

@Data
public abstract class Shrine implements RoomContent {
    protected String id;
    protected ExpirableAdditionEffect effect;

    @Override
    public Weight getRoomContentWeight() {
        return effect.getWeight();
    }
}
