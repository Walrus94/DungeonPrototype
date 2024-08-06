package org.dungeon.prototype.model.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.service.PlayerLevelService;
import org.springframework.data.annotation.Transient;

import java.util.EnumMap;
import java.util.List;

import static org.apache.commons.lang3.math.NumberUtils.max;
import static org.apache.commons.lang3.math.NumberUtils.min;
import static org.dungeon.prototype.model.effect.Action.ADD;

@Data
@Slf4j
@NoArgsConstructor
public class Player {
    private String id;
    private Long chatId;
    private String nickname;
    private Point currentRoom;
    private String currentRoomId;
    private Direction direction;
    private Integer gold;
    private Long xp;
    private Integer playerLevel;
    private Long nextLevelXp;
    private Integer hp;
    private Integer maxHp;
    private Integer mana;
    private Integer maxMana;
    private Integer defense;
    private Integer maxDefense;
    private Inventory inventory;
    private List<PlayerEffect> playerEffects;
    EnumMap<PlayerAttribute, Integer> attributes;

    //TODO: move to service and use queries through repository
    @Transient
    public void addXp(Integer xpReward) {
        xp += xpReward;
        playerLevel = PlayerLevelService.getLevel(xp);
    }
    public void decreaseDefence(int amount) {
        defense -= amount;
    }
    public void decreaseHp(int amount) {
        hp -= amount;
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
    public void restoreArmor() {
        defense = maxDefense;
    }

    public void addEffects(List<PlayerEffect> effects) {
        effects.forEach(playerEffect -> {
            switch (playerEffect.getAttribute()) {
                case HEALTH -> hp = playerEffect.getAction().equals(ADD) ?
                        min(maxHp, max(0, hp + playerEffect.getAmount())) :
                        min(maxHp, max(0, ((Double)(hp * playerEffect.getMultiplier())).intValue()));
                case MANA -> mana = playerEffect.getAction().equals(ADD) ?
                        min(maxMana, max(0, mana + playerEffect.getAmount())) :
                        min(maxMana, max(0, ((Double)(mana * playerEffect.getMultiplier())).intValue()));
                case ARMOR -> defense = playerEffect.getAction().equals(ADD) ?
                        min(maxDefense, max(0, defense + playerEffect.getAmount())) :
                        min(maxDefense, max(0, ((Double)(defense * playerEffect.getMultiplier())).intValue()));
            }
            effects.add(playerEffect);
        });
    }
}
