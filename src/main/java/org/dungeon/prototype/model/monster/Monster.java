package org.dungeon.prototype.model.monster;

import lombok.Builder;
import lombok.Data;
import lombok.val;
import org.apache.commons.math3.util.FastMath;
import org.dungeon.prototype.model.effect.ExpirableAdditionEffect;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.util.GenerationUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.dungeon.prototype.model.effect.attributes.EffectAttribute.MOVING;
import static org.dungeon.prototype.util.RoomGenerationUtils.calculateMonsterWeight;


@Data
@Builder
public class Monster {
    private String id;
    private MonsterClass monsterClass;

    private Integer hp;
    private Integer maxHp;
    private Weight weight;
    private MonsterAttack primaryAttack;
    private MonsterAttack secondaryAttack;

    private MagicType magicType;

    private List<ExpirableAdditionEffect> effects;
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

    public Weight getWeight() {
        return isNull(weight) ? weight = effects.stream().map(ExpirableAdditionEffect::getWeight).reduce(Weight::add).
                orElse(new Weight()).add(calculateMonsterWeight(
                        hp, maxHp,
                        primaryAttack,
                        secondaryAttack)) : weight;
    }

    public Integer getXpReward() {
        return (int) getWeight().toVector().getNorm();
    }

    public void decreaseHp(Integer amount) {
        hp = FastMath.max(hp - amount, 0);
    }

    public void addEffect(ExpirableAdditionEffect monsterEffect) {
        switch (monsterEffect.getAttribute()) {
            case ATTACK, HEALTH -> effects.add(monsterEffect);
            case MOVING -> {
                if (effects.stream().anyMatch(effect -> MOVING.equals(effect.getAttribute()))) {
                    val oldEffect = effects.stream().filter(e -> MOVING.equals(e.getAttribute()))
                            .findFirst();
                    if (oldEffect.isPresent() && !oldEffect.get().isPermanent()) {
                        oldEffect.get().setTurnsLeft(oldEffect.get().getTurnsLeft());
                    }
                } else {
                    effects.add(monsterEffect);
                }
            }
        }
    }
}
