package org.dungeon.prototype.model.room.content;

import lombok.Builder;
import lombok.Data;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

@Data
@Builder
public class Anvil implements RoomContent {
    private String id;
    private Double chanceToBreakWeapon;
    private Integer attackBonus;
    private boolean armorRestored;

    @Override
    public Weight getRoomContentWeight() {
        return Weight.builder()
                .armor(1.0)
                .attack(attackBonus * (1 - chanceToBreakWeapon))
                .build();
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.ANVIL;
    }
}
