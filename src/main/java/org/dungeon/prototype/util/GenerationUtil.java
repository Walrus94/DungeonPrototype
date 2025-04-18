package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.weight.Weight;
import org.springframework.beans.factory.annotation.Value;

import java.util.BitSet;

import static org.apache.commons.math3.util.FastMath.sqrt;

@Slf4j
@UtilityClass
public class GenerationUtil {

    @Value("${generation.items.selling-price-ratio}")
    private double sellingPriceRatio;
    @Value("${generation.items.buying-price-ratio}")
    private double buyingPriceRatio;

    public static double getSellingPriceRatio() {
        return sellingPriceRatio;
    }

    public static double getBuyingPriceRatio() {
        return buyingPriceRatio;
    }

    public static void applyAdjustment(Weapon weapon, double[] adjustmentAttributes) {
        weapon.setAttack((int) (weapon.getAttack() * adjustmentAttributes[0]));
        weapon.setChanceToMiss(weapon.getChanceToMiss() * adjustmentAttributes[1]);
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * adjustmentAttributes[2]);
        weapon.setCriticalHitMultiplier(weapon.getCriticalHitMultiplier() * adjustmentAttributes[3]);
        weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * adjustmentAttributes[4]);
    }

    public static BitSet getDefaultAttackPattern() {
        return BitSet.valueOf(new byte[]{1, 1, 1, 0});
    }

    public static Weight getWeightLimitNormalization(Weight expectedWeight, Double limitWeightAbs, Integer currentStep, Integer totalRooms) {
        val expectedWeightAbs = expectedWeight.toVector().getNorm();
        val limit = totalRooms * limitWeightAbs * (1 - sqrt(currentStep.doubleValue() / (totalRooms + 1)));
        if (expectedWeightAbs > limit) {
            return expectedWeight.multiply(limit / expectedWeightAbs);
        }
        return expectedWeight;
    }

    public static Weight calculateWeaponWeight(Integer attack,
                                               Double criticalHitChance,
                                               Double criticalHitMultiplier,
                                               Double chanceToKnockOut,
                                               Double chanceToMiss,
                                               MagicType magicType) {
        return Weight.builder()
                .criticalHitChance(criticalHitChance)
                .criticalHitMultiplier(criticalHitMultiplier)
                .attack((1.0 - chanceToMiss) * attack)
                .chanceToKnockout(chanceToKnockOut)
                .divineMagic(magicType.getDivineMagic())
                .arcaneMagic(magicType.getArcaneMagic())
                .build();
    }

    public static Weight calculateWearableWeight(Integer armor, Double chanceToDodge, MagicType magicType) {
        return Weight.builder()
                .armor(Double.valueOf(armor))
                .chanceToDodge(chanceToDodge * armor)
                .divineMagic(magicType.getDivineMagic())
                .arcaneMagic(magicType.getArcaneMagic())
                .build();
    }
}
