package org.dungeon.prototype.model.monster;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.weight.Weight;

import static java.util.Objects.nonNull;

@Data
@Builder
public class MonsterAttack {
    private MonsterAttackType attackType;
    private Integer attack;

    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private Effect effect;
    private Double causingEffectProbability;

    public Weight getWeight() {
        return Weight.builder()
                .attack(1.0 - chanceToMiss)
                .chanceToKnockout(chanceToKnockOut)
                .build().add( nonNull(effect) ?
                        Weight.fromVector((ArrayRealVector) effect.getWeight().getNegative().toVector()
                                .mapMultiply(causingEffectProbability)) :
                        new Weight());
    }
}
