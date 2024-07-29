package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.MagicType;
import org.dungeon.prototype.model.inventory.attributes.effect.Effect;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;

import java.util.List;

@Data
@NoArgsConstructor
public class Weapon implements Item {
    private String id;
    private Long chatId;
    private WeaponAttributes attributes;
    private String name;
    private Integer weight;

    private Integer attack;
    private Integer additionalFirstHit;
    private Double criticalHitChance;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private boolean isCompleteDragonBone;

    private boolean hasMagic;
    private MagicType magicType;

    private List<Effect> effects;

    private Integer sellingPrice;
    private Integer buyingPrice;

    @Override
    public ItemType getItemType() {
        return ItemType.WEAPON;
    }
}
