package org.dungeon.prototype.model.room.content;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.room.RoomType;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Treasure extends ItemsRoom {
    @NotNull
    @Positive
    private Integer gold;
    @Override
    public Integer getRoomContentWeight() {
        return gold + items.stream().mapToInt(Item::getWeight).sum();
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.TREASURE;
    }
}
