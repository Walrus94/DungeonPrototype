package org.dungeon.prototype.model.room.content;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.weight.Weight;

import static org.dungeon.prototype.util.RoomGenerationUtils.convertToRoomType;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MonsterRoom implements RoomContent {
    private String id;
    private Monster monster;

    @Override
    public Weight getRoomContentWeight() {
        return monster.getWeight().getNegative();
    }

    @Override
    public RoomType getRoomType() {
        return convertToRoomType(monster.getMonsterClass());
    }
}
