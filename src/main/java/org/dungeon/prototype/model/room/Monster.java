package org.dungeon.prototype.model.room;

import lombok.Data;

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
    public Room.Type getRoomType() {
        return Room.Type.MONSTER;
    }
}
