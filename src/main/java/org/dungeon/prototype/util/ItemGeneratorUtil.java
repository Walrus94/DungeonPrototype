package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.inventory.items.Weapon;

@Slf4j
@UtilityClass
public class ItemGeneratorUtil {

    public static int calculateWeight(Weapon weapon) {
        var weight = weapon.getAttack() + weapon.getAdditionalFirstHit();
        if (weapon.getCriticalHitChance() > 0.0) {
            weight *= weapon.getCriticalHitChance() * 100;
        }
        if (weapon.getCriticalHitChance() > 0.0) {
            weight *= weapon.getCriticalHitChance() * 100;
        }
        if (weapon.getChanceToKnockOut() > 0.0) {
            weight *= weapon.getChanceToKnockOut() * 100;
        }
        if (weapon.getChanceToMiss() > 0.0) {
            weight /= weapon.getChanceToMiss() * 100;
        }
        log.debug("Calculated weight: {}", weight);
        return weight;
    }

    public static void multiplyAllParametersBy(Weapon weapon, double multiplier) {
        weapon.setAttack((int) (weapon.getAttack() * multiplier));
        weapon.setChanceToKnockOut(weapon.getChanceToKnockOut() * multiplier);
        weapon.setCriticalHitChance(weapon.getCriticalHitChance() * multiplier);
        weapon.setChanceToMiss(weapon.getChanceToMiss() / multiplier);
        weapon.setAdditionalFirstHit((int) (weapon.getAdditionalFirstHit() * multiplier));
    }
}
