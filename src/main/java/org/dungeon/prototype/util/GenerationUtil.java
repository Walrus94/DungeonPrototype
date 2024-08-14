package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.effect.Action;
import org.dungeon.prototype.model.inventory.items.Weapon;

import java.util.BitSet;

@Slf4j
@UtilityClass
public class GenerationUtil {

    public static int calculateWeight(Weapon weapon) {
        var weight = weapon.getAttack();
        if (weapon.getCriticalHitChance().intValue() * 100 > 0) {
            weight *= weapon.getCriticalHitChance().intValue() * 100;
        }
        if (weapon.getChanceToKnockOut().intValue() * 100 > 0) {
            weight *= weapon.getChanceToKnockOut().intValue() * 100;
        }
        if (weapon.getChanceToMiss().intValue() * 100 > 0) {
            weight /= weapon.getChanceToMiss().intValue() * 100;
        }
        log.debug("Calculated weight: {}", weight);
        return weight;
    }

    public static void multiplyAllParametersBy(Weapon weapon, double multiplier) {
        weapon.setAttack((int) (weapon.getAttack() * multiplier));
        weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * multiplier);
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * multiplier);
        weapon.setChanceToMiss(weapon.getChanceToMiss() / multiplier);
    }

    public static BitSet getDefaultAttackPattern() {
        return BitSet.valueOf(new byte[]{1, 1, 1, 0});
    }

    public static boolean isNegative(Action action, int amount, double multiplier) {
        return switch (action) {
            case ADD -> amount < 0;
            case MULTIPLY -> multiplier < 1.0;
        };
    }
}
