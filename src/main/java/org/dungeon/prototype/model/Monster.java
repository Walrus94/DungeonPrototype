package org.dungeon.prototype.model;

import lombok.Data;

@Data
public class Monster {
    private Integer attack;
    private Integer maxHp;
    private Integer hp;
    private Integer xpReward;

    public Monster(Integer attack, Integer maxHp, Integer xpReward) {
        this.attack = attack;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.xpReward = xpReward;
    }
}
