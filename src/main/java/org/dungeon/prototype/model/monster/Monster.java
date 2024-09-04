package org.dungeon.prototype.model.monster;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.math3.util.FastMath;
import org.dungeon.prototype.model.effect.ExpirableEffect;
import org.dungeon.prototype.util.GenerationUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.*;


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

    private List<ExpirableEffect> effects;
    private LinkedList<MonsterAttack> attackPattern;

    private MonsterAttack currentAttack;
    public LinkedList<MonsterAttack> getDefaultAttackPattern() {
        return GenerationUtil.getDefaultAttackPattern().stream().mapToObj(value -> {
            if (value == 1) {
                return getSecondaryAttack();
            } else {
                return getPrimaryAttack();
            }
        }).collect(Collectors.toCollection(LinkedList::new));
    }

    public Integer getWeight() {
        return -xpReward; //TODO: consider different formula
    }

    public void decreaseHp(Integer amount) {
        hp = FastMath.max(hp - amount, 0);
    }

    public void addEffect(ExpirableEffect monsterEffect) {
        switch (monsterEffect.getAttribute()) {
            case ATTACK, HEALTH -> effects.add(monsterEffect);
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
