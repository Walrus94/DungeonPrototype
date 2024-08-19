package org.dungeon.prototype.model.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.effect.DirectPlayerEffect;
import org.dungeon.prototype.model.effect.ItemEffect;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.dungeon.prototype.model.inventory.Inventory;

import java.util.EnumMap;
import java.util.List;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.util.FastMath.max;
import static org.dungeon.prototype.model.effect.Action.ADD;
import static org.dungeon.prototype.model.effect.Action.MULTIPLY;

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
    private Integer primaryAttack;
    private Integer secondaryAttack;
    private Inventory inventory;
    private List<PlayerEffect> effects;
    private EnumMap<PlayerAttribute, Integer> attributes;

    //TODO: move to service and use queries through repository
    public void addXp(Integer xpReward) {
        xp += xpReward;
        log.debug("Rewarded xp: {}, total: {}", xpReward, xp);
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

    public void addEffects(List<PlayerEffect> effects) {
        if (isNull(this.effects)) {
            this.effects = effects;
        } else {
            this.effects.addAll(effects);
        }
    }

    public void removeItemEffects(List<ItemEffect> effects) {
        effects.forEach(effect -> {
            if (this.effects.remove(effect)) {
                switch (effect.getAction()) {
                    case ADD -> addCounterEffect(effect.getAttribute(), effect.getAmount());
                    case MULTIPLY -> addCounterEffect(effect.getAttribute(), effect.getMultiplier());
                }
            }
        });
    }

    private boolean addCounterEffect(PlayerEffectAttribute attribute, Double multiplier) {
        val counterEffect = new DirectPlayerEffect();
        counterEffect.setAction(MULTIPLY);
        counterEffect.setAttribute(attribute);
        counterEffect.setMultiplier(1 / multiplier);
        counterEffect.setTurnsLasts(1);
        counterEffect.setIsAccumulated(true);
        return this.effects.add(counterEffect);
    }

    private boolean addCounterEffect(PlayerEffectAttribute attribute, Integer amount) {
        val counterEffect = new DirectPlayerEffect();
        counterEffect.setAction(ADD);
        counterEffect.setAttribute(attribute);
        counterEffect.setAmount(-amount);
        counterEffect.setTurnsLasts(1);
        counterEffect.setIsAccumulated(true);
        return this.effects.add(counterEffect);
    }
}
