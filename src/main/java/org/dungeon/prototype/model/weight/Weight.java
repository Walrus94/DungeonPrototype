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
    Double hp = 0.0;
    @Builder.Default
    Double maxHp = 0.0;
    @Builder.Default
    Double mana = 0.0;
    @Builder.Default
    Double maxMana = 0.0;
    @Builder.Default
    Double armor = 0.0;
    @Builder.Default
    Double maxArmor = 0.0;
    @Builder.Default
    Double chanceToDodge = 0.0;
    @Builder.Default
    Double goldBonus = 0.0;
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
                hp, maxHp, mana, maxMana, armor,
                maxArmor, chanceToDodge, goldBonus, xpBonus,
                attack, criticalHitChance, chanceToKnockout,
                arcaneMagic, divineMagic
        });
    }

    public ArrayRealVector getHealthSubVector() {
        return new ArrayRealVector(new double[] {hp, maxHp});
    }

    public ArrayRealVector getMagicSubVector() {
        return new ArrayRealVector(new double[] {mana, maxMana, arcaneMagic, divineMagic});
    }

    public ArrayRealVector getDefenseSubVector() {
        return new ArrayRealVector(new double[] {armor, maxArmor, chanceToDodge});
    }

    public ArrayRealVector getAttackSubVector() {
        return new ArrayRealVector(new double[] {attack, criticalHitChance, criticalHitMultiplier, chanceToKnockout});
    }

    public ArrayRealVector getBonusSubVector() {
        return new ArrayRealVector(new double[] {goldBonus, xpBonus});
    }
    public static Weight fromVector(ArrayRealVector vector) {
        return Weight.builder()
                .hp(vector.getDataRef()[0])
                .maxHp(vector.getDataRef()[1])
                .mana(vector.getDataRef()[2])
                .maxMana(vector.getDataRef()[3])
                .armor(vector.getDataRef()[4])
                .maxArmor(vector.getDataRef()[5])
                .chanceToDodge(vector.getDataRef()[6])
                .goldBonus(vector.getDataRef()[7])
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
            case HEALTH -> Weight.builder().hp(value).build();
            case HEALTH_MAX -> Weight.builder().maxHp(value).build();
            case HEALTH_MAX_ONLY -> Weight.builder().hp(1/value).maxHp(value).build();

            case MANA -> Weight.builder().mana(value).build();
            case MANA_MAX -> Weight.builder().maxMana(value).build();
            case MANA_MAX_ONLY -> Weight.builder().mana(1/value).maxMana(value).build();

            case MAX_ARMOR -> Weight.builder().armor(1/value).maxArmor(value).build();
            case CHANCE_TO_DODGE -> Weight.builder().chanceToDodge(value).build();

            case GOLD_BONUS -> Weight.builder().goldBonus(value).build();
            case XP_BONUS -> Weight.builder().xpBonus(value).build();

            case ATTACK -> Weight.builder()
                    .attack(value)
                    .chanceToKnockout(value)
                    .criticalHitChance(value)
                    .criticalHitMultiplier(value)
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
