package org.dungeon.prototype.model.room.content;

import lombok.Data;
import org.dungeon.prototype.model.effect.ExpirableEffect;

@Data
public abstract class Shrine implements RoomContent {
    protected String id;
    protected ExpirableEffect effect;

    @Override
    public Integer getRoomContentWeight() {
        return effect.getWeight();
    }
}
