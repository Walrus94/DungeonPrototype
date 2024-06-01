package org.dungeon.prototype.model.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.EnumMap;

@Data
@NoArgsConstructor
@Document(collection = "players")
public class Player {

    @Id
    private Long chatId;
    private String nickname;
    private Point currentRoom;
    private String currentRoomId;
    private Direction direction;
    private Integer gold;
    private Long xp;
    private Integer hp;
    private Integer maxHp;
    private Integer mana;
    private Integer maxMana;
    private Integer attack;
    private Integer defense;
    private Integer maxDefense;
    private ArmorSet armor;
    private WeaponSet weapon;

    EnumMap<Attribute, Integer> attributes;

    //TODO: move to service and use queries through repository
    @Transient
    public void addXp(Integer xpReward) {
        xp += xpReward;
    }

    @Transient
    public void decreaseHp(int amount) {
        hp -= amount;
    }
    @Transient
    public void decreaseDefence(int amount) {
        defense -= amount;
    }
    @Transient
    public void addGold(int amount) {
        gold += amount;
    }
    @Transient
    public void refillHp() {
        hp = maxHp;
    }
    @Transient
    public void refillMana() {
        mana = maxMana;
    }
}
