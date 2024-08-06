package org.dungeon.prototype.model.room.content;

import lombok.Data;
import org.dungeon.prototype.model.effect.Effect;

@Data
public abstract class Shrine implements RoomContent {
    protected String id;
    protected Effect regeneration;
}
