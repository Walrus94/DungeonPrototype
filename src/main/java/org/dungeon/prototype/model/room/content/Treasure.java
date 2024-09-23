package org.dungeon.prototype.model.room.content;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Treasure extends ItemsRoom {
    @NotNull
    @Positive
    private Integer gold;
    @Override
    public Weight getRoomContentWeight() {
        //TODO: verify formula
        return items.stream().map(Item::getWeight).reduce(
                Weight.builder().goldBonusToGold(gold.doubleValue()).build(), Weight::add);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.TREASURE;
    }
}
