package org.dungeon.prototype.model.room.content;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.room.RoomType;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Merchant extends BonusRoom {
    @Override
    public Integer getRoomContentWeight() {
        return items.stream().mapToInt(Item::getWeight).sum();
    }
    @Override
    public RoomType getRoomType() {
        return RoomType.MERCHANT;
    }
}
