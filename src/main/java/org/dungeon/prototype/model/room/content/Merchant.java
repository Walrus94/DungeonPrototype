package org.dungeon.prototype.model.room.content;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Merchant extends ItemsRoom {
    @Override
    public Weight getRoomContentWeight() {
        return items.stream().map(Item::getWeight).reduce(Weight::add).orElse(new Weight());
    }
    @Override
    public RoomType getRoomType() {
        return RoomType.MERCHANT;
    }
}
