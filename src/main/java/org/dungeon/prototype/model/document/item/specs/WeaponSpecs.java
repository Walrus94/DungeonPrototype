package org.dungeon.prototype.model.document.item.specs;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemSpecs;

@Data
public class WeaponSpecs implements ItemSpecs {
    private Integer attack;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private boolean isCompleteDragonBone;
}
