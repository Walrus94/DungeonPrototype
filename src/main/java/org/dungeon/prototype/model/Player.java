package org.dungeon.prototype.model;

import lombok.Data;
import org.dungeon.prototype.util.LevelUtil;

import java.io.Serial;
import java.io.Serializable;

@Data
public class Player implements Serializable {
    public Player(Point currentRoom, LevelUtil.Direction direction) {
        this.currentRoom = currentRoom;
        this.direction = direction;
        this.gold = 100;
        this.attack = 5;
        this.defense = 2;
        this.xp = 0L;
        this.hp = 500;
        this.maxHp = 500;
        this.mana = 10;
        this.maxMana = 10;
    }

    @Serial
    private static final long serialVersionUID = 6523075017967757691L;
    private Point currentRoom;
    private LevelUtil.Direction direction;
    private Integer gold;
    private Long xp;
    private Integer hp;
    private Integer maxHp;
    private Integer mana;
    private Integer maxMana;
    private Integer attack;
    private Integer defense;

    public void addXp(Integer xpReward) {
        xp += xpReward;
    }

    public void decreaseHp(int amount) {
        hp -= amount;
    }

    public void decreaseDefence(int amount) {
        defense -= amount;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public void refillHp() {
        hp = maxHp;
    }

    public void refillMana() {
        mana = maxMana;
    }
}
