package org.dungeon.prototype.model.room.content;

import lombok.Data;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

@Data
public class Anvil implements RoomContent {
    private String id;
    private Weight roomContentWeight;
    private Double chanceToBreakWeapon;
    private Integer attackBonus;
    private boolean armorRestored;

    @Override
    public RoomType getRoomType() {
        return RoomType.ANVIL;
    }
}
