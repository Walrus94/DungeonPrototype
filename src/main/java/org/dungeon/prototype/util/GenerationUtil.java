package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.properties.WeaponGenerationProperties;
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

    public static void applyAdjustment(Weapon weapon, WeaponGenerationProperties.AdjustmentAttributes adjustmentAttributes) {
        weapon.setAttack((int) (weapon.getAttack() * adjustmentAttributes.getAttackRatio()));
        weapon.setChanceToMiss(weapon.getChanceToMiss() * adjustmentAttributes.getChanceToMissRatio());
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * adjustmentAttributes.getCriticalChanceRatio());
        weapon.setCriticalHitMultiplier(weapon.getCriticalHitMultiplier() * adjustmentAttributes.getCriticalMultiplierRatio());
        weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * adjustmentAttributes.getKnockOutChanceRatio());
    }
    public static void multiplyAllParametersBy(Weapon weapon, double multiplier) {
        weapon.setAttack((int) (weapon.getAttack() * multiplier));
        weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * multiplier);
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * multiplier);
        weapon.setCriticalHitMultiplier(weapon.getCriticalHitMultiplier() * multiplier);
        weapon.setChanceToMiss(weapon.getChanceToMiss() / multiplier);
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
                .armorToMaxArmor(Double.valueOf(armor))
                .chanceToDodge(chanceToDodge * armor)
                .divineMagic(magicType.getDivineMagic())
                .arcaneMagic(magicType.getArcaneMagic())
                .build();
    }
}
