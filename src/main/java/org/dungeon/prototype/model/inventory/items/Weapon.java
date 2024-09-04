package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.PermanentEffect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class Weapon extends Item {
    private WeaponAttributes attributes;

    private Integer attack;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private Boolean isCompleteDragonBone;

    private List<PermanentEffect> effects;

    @Override
    public ItemType getItemType() {
        return ItemType.WEAPON;
    }
}
