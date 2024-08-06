package org.dungeon.prototype.model.document.item.specs;

import lombok.Data;
import org.dungeon.prototype.model.document.item.ItemSpecs;

@Data
public class WeaponSpecs implements ItemSpecs {
    private Integer attack;
    private Integer additionalFirstHit;
    private Double criticalHitChance;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private boolean isCompleteDragonBone;
}
