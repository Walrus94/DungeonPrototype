package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.ItemEffect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class Weapon extends Item {
    private WeaponAttributes attributes;

    private Integer attack;
    private Integer additionalFirstHit;
    private Double criticalHitChance;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private boolean isCompleteDragonBone;

    private List<ItemEffect> effects;

    @Override
    public ItemType getItemType() {
        return ItemType.WEAPON;
    }
}
