package org.dungeon.prototype.model.weight;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.dungeon.prototype.model.effect.attributes.EffectAttribute;

@Value
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Weight {
    @Builder.Default
    Double hpToMaxHp = 0.0;
    @Builder.Default
    Double hpDeficiencyToMaxHp  = 0.0;
    @Builder.Default
    Double manaToMaxMana = 0.0;
    @Builder.Default
    Double manaDeficiencyToMaxMana = 0.0;
    @Builder.Default
    Double armorToMaxArmor = 0.0;
    @Builder.Default
    Double armorDeficiencyToMaxArmor = 0.0;
    @Builder.Default
    Double chanceToDodge = 0.0;
    @Builder.Default
    Double goldBonusToGold = 0.0;
    @Builder.Default
    Double xpBonus = 0.0;
    @Builder.Default
    Double attack = 0.0;
    @Builder.Default
    Double criticalHitChance = 0.0;
    @Builder.Default
    Double criticalHitMultiplier = 0.0;
    @Builder.Default
    Double chanceToKnockout = 0.0;
    @Builder.Default
    Double arcaneMagic = 0.0;
    @Builder.Default
    Double divineMagic = 0.0;

    public ArrayRealVector toVector() {
        return new ArrayRealVector(new double[] {
                hpToMaxHp, hpDeficiencyToMaxHp, manaToMaxMana, manaDeficiencyToMaxMana, armorToMaxArmor,
                armorDeficiencyToMaxArmor, chanceToDodge, goldBonusToGold, xpBonus,
                attack, criticalHitChance, chanceToKnockout,
                arcaneMagic, divineMagic
        });
    }

    public static Weight fromVector(ArrayRealVector vector) {
        return Weight.builder()
                .hpToMaxHp(vector.getDataRef()[0])
                .hpDeficiencyToMaxHp(vector.getDataRef()[1])
                .manaToMaxMana(vector.getDataRef()[2])
                .manaDeficiencyToMaxMana(vector.getDataRef()[3])
                .armorToMaxArmor(vector.getDataRef()[4])
                .armorDeficiencyToMaxArmor(vector.getDataRef()[5])
                .chanceToDodge(vector.getDataRef()[6])
                .goldBonusToGold(vector.getDataRef()[7])
                .xpBonus(vector.getDataRef()[8])
                .attack(vector.getDataRef()[9])
                .criticalHitChance(vector.getDataRef()[10])
                .chanceToKnockout(vector.getDataRef()[11])
                .arcaneMagic(vector.getDataRef()[12])
                .divineMagic(vector.getDataRef()[13])
                .build();
    }
    public static Weight buildWeightVectorForAttribute(EffectAttribute attribute, double value) {
        return switch (attribute) {
            case HEALTH -> Weight.builder().hpToMaxHp(value).hpDeficiencyToMaxHp(-1/value).build();
            case HEALTH_MAX -> Weight.builder().hpDeficiencyToMaxHp(1/value).build();
            case HEALTH_MAX_ONLY -> Weight.builder().hpToMaxHp(-value).hpDeficiencyToMaxHp(1/value).build();

            case MANA -> Weight.builder().manaToMaxMana(value).manaDeficiencyToMaxMana(-1/value).build();
            case MANA_MAX -> Weight.builder().manaToMaxMana(1/value).build();
            case MANA_MAX_ONLY -> Weight.builder().manaToMaxMana(-value).manaDeficiencyToMaxMana(1/value).build();

            case MAX_ARMOR -> Weight.builder().armorToMaxArmor(value).armorDeficiencyToMaxArmor(-1/value).build();
            case CHANCE_TO_DODGE -> Weight.builder().chanceToDodge(value).build();

            case GOLD_BONUS -> Weight.builder().goldBonusToGold(value).build();
            case XP_BONUS -> Weight.builder().xpBonus(value).build();

            //TODO: verify
            case ATTACK -> Weight.builder()
                    .attack(value)
                    .chanceToKnockout(value)
                    .criticalHitChance(value)
                    .chanceToKnockout(value)
                    .build();
            case CRITICAL_HIT_CHANCE -> Weight.builder().criticalHitChance(value).build();
            case CRITICAL_HIT_MULTIPLIER -> Weight.builder().criticalHitMultiplier(value).build();
            case MISS_CHANCE -> Weight.builder().attack(1 - value).build();
            case KNOCK_OUT_CHANCE -> Weight.builder().chanceToKnockout(value).build();
            case MOVING -> new Weight();//TODO: fix
        };
    }

    public Weight add(Weight weight) {
        return Weight.fromVector(this.toVector().add(weight.toVector()));
    }

    public Weight multiply(Double multiplier) {
        return Weight.fromVector((ArrayRealVector) toVector().mapMultiply(multiplier));
    }

    public Weight getNegative() {
        return Weight.fromVector((ArrayRealVector) this.toVector().mapMultiply(-1.0));
    }
}
