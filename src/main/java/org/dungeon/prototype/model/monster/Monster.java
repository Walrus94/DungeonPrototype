package org.dungeon.prototype.model.monster;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.effect.MonsterEffect;
import org.dungeon.prototype.util.GenerationUtil;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.dungeon.prototype.model.effect.attributes.MonsterEffectAttribute.MOVING;

@Data
@NoArgsConstructor
public class Monster {
    private String id;
    private MonsterClass monsterClass;
    private Integer level;

    private Integer hp;
    private Integer maxHp;
    private Integer xpReward;

    private MonsterAttack primaryAttack;
    private MonsterAttack secondaryAttack;

    private List<MonsterEffect> effects;

    private Iterator<MonsterAttack> currentAttack;
    public List<MonsterAttack> getDefaultAttackPattern() {
        return GenerationUtil.getDefaultAttackPattern().stream().mapToObj(value -> {
            if (value == 1) {
                return getSecondaryAttack();
            } else {
                return getPrimaryAttack();
            }
        }).collect(Collectors.toList());
    }

    public Integer getWeight() {
        return -xpReward; //TODO: consider different formula
    }

    public void decreaseHp(Integer amount) {
        hp -= amount;
    }

    public void addEffect(MonsterEffect monsterEffect) {
        switch (monsterEffect.getAttribute()) {
            case ATTACK -> {

            }
            case HEALTH -> {
            }
            case MOVING -> {
                if (effects.stream().anyMatch(effect -> MOVING.equals(effect.getAttribute()))) {
                    effects.stream().filter(effect -> MOVING.equals(effect.getAttribute()))
                            .findFirst().get().setTurnsLasts(monsterEffect.getTurnsLasts());
                } else {
                    effects.add(monsterEffect);
                }
            }
        }
    }
}
