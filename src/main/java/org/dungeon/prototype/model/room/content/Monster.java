package org.dungeon.prototype.model.room.content;

import lombok.Data;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;

@Data
public class Monster implements RoomContent {
    private Integer level;
    private Integer attack;
    private Integer maxHp;
    private Integer hp;
    private Integer xpReward;

    public Monster(Integer level) {
        this.attack = 5 * level;
        this.maxHp = 10 * level;
        this.level = level;
        this.hp = maxHp;
        this.xpReward = attack + maxHp * 10;
    }

    @Override
    public Integer getRoomContentWeight() {
        return -level * 100;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.MONSTER;
    }
}
