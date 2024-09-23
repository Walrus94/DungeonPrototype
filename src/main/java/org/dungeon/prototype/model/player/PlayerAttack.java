package org.dungeon.prototype.model.player;

import lombok.Data;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;

@Data
public class PlayerAttack {
    private Integer attack;
    private WeaponAttackType attackType;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
}
