package org.dungeon.prototype.model.document.player;

import lombok.Data;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttackType;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class PlayerAttackDocument {
    private Integer attack;
    private WeaponAttackType attackType;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
}
