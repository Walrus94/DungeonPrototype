package org.dungeon.prototype.model.room.content;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;

@Data
@NoArgsConstructor
public class Monster implements RoomContent {
    private Integer level;
    private Integer attack;
    private Integer maxHp;
    private Integer hp;
    private Integer xpReward;

    @Override
    public Integer getRoomContentWeight() {
        return -level * 100;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.MONSTER;
    }
}
