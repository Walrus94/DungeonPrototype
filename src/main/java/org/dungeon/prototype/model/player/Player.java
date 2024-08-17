package org.dungeon.prototype.model.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.effect.PlayerEffect;
import org.dungeon.prototype.model.effect.attributes.PlayerEffectAttribute;
import org.dungeon.prototype.model.inventory.Inventory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
    private Inventory inventory;
    private Map<PlayerEffectAttribute, PriorityQueue<PlayerEffect>> effects = new HashMap<>();
    EnumMap<PlayerAttribute, Integer> attributes;

    //TODO: move to service and use queries through repository
    public void addXp(Integer xpReward) {
        xp += xpReward;
        log.debug("Rewarded xp: {}, total: {}", xpReward, xp);
    }
    public void decreaseDefence(int amount) {
        defense -= amount;
    }
    public void decreaseHp(int amount) {
        hp = max(0, hp - amount);
    }
    public void addGold(int amount) {
        gold += amount;
    }
    public void removeGold(int amount) {
        gold = amount > gold ? 0 : gold - amount;
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

    public Integer getAttack(Boolean isPrimaryWeapon) {
        var weaponSet = inventory.getWeaponSet();
        Integer attack = attributes.get(PlayerAttribute.POWER);
        if (isPrimaryWeapon) {
            if (nonNull(weaponSet.getPrimaryWeapon())) {
                attack += weaponSet.getPrimaryWeapon().getAttack();
            }
        } else {
            attack += nonNull(weaponSet.getSecondaryWeapon()) ? weaponSet.getSecondaryWeapon().getAttack() : 0;
        }
        attack = applyEffects(attack);
        return attack;
    }

    private Integer applyEffects(Integer attack) {
        val effectsQueue = effects.get(PlayerEffectAttribute.ATTACK);
        if (isNull(effectsQueue)) {
            return attack;
        }
        val effectsMap = effectsQueue.stream()
                .filter(Effect::isApplicable)
                .collect(Collectors.groupingBy(Effect::getAction));
        return effectsMap.values().stream().mapToInt(playerEffects -> {
            Double multiplyFactor = playerEffects.stream()
                    .filter(e -> MULTIPLY.equals(e.getAction()))
                    .map(Effect::getMultiplier)
                    .reduce(attack.doubleValue(), (m1, m2) -> m1 * m2);
            return playerEffects.stream()
                    .filter(e -> ADD.equals(e.getAction()))
                    .map(Effect::getAmount)
                    .reduce(multiplyFactor.intValue(), Integer::sum);
        }).findFirst().orElse(attack);
    }

    public void addEffects(List<PlayerEffect> effects) {
        effects.forEach(playerEffect ->
                this.effects.get(playerEffect.getAttribute()).offer(playerEffect));
    }
}
