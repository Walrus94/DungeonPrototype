package org.dungeon.prototype.model.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.service.PlayerLevelService;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.max;

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
    private PlayerAttack primaryAttack;
    private PlayerAttack secondaryAttack;
    private Double chanceToDodge;
    private Double xpBonus = 1.0;
    private Double goldBonus = 1.0;
    private Inventory inventory;
    private List<Effect> effects;
    private EnumMap<PlayerAttribute, Integer> attributes;

    public boolean addXp(Integer xpReward) {
        double reward = xpReward * xpBonus;
        xp = xp + (int) reward;
        log.info("Rewarded xp: {}, total: {}", xpReward, xp);
        if (xp > nextLevelXp) {
            playerLevel++;
            log.info("Level {} achieved!", playerLevel);
            refillHp();
            refillMana();
            nextLevelXp = PlayerLevelService.calculateXPForLevel(playerLevel);
            return true;
        } else {
            return false;
        }
    }

    public void decreaseDefence(int amount) {
        defense = max(0, defense - amount);
    }

    public void decreaseHp(int amount) {
        hp = max(0, hp - amount);
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public void removeGold(int amount) {
        gold = max(0, gold - amount);
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

    public void addEffects(List<Effect> effects) {
        if (isNull(this.effects)) {
            this.effects = effects;
        } else {
            this.effects.addAll(effects);
        }
    }

    public <T extends Effect> void addEffect(T effect) {
        if (isNull(this.effects)) {
            this.effects = new ArrayList<>();
        }
        this.effects.add(effect);
    }

    public <T extends Effect> boolean removeEffects(List<T> effects) {
        return this.effects.removeAll(effects);
    }

    public Weight getWeight() {
        return Weight.builder()
                .hp((double) hp)
                .maxHp((double) maxHp)
                .mana((double) mana)
                .maxMana((double) maxMana)
                .armor((double) defense)
                .maxArmor((double) maxDefense)
                .chanceToDodge(chanceToDodge)
                .goldBonus(goldBonus)
                .xpBonus(xpBonus)
                .attack((1.0 - primaryAttack.getChanceToMiss()) * primaryAttack.getAttack() +
                        (nonNull(secondaryAttack) ? (1.0 - secondaryAttack.getChanceToMiss()) * secondaryAttack.getAttack() : 0.0))
                .criticalHitChance(primaryAttack.getCriticalHitChance() +
                        (nonNull(secondaryAttack) ? secondaryAttack.getCriticalHitChance() : 0.0))
                .criticalHitMultiplier(primaryAttack.getCriticalHitMultiplier() +
                        (nonNull(secondaryAttack) ? secondaryAttack.getCriticalHitMultiplier() : 0.0))
                .chanceToKnockout(primaryAttack.getChanceToKnockOut() +
                        (nonNull(secondaryAttack) ? secondaryAttack.getChanceToKnockOut() : 0.0))
                .build();
    }
}
