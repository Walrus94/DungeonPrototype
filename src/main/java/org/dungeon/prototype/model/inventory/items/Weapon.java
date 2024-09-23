package org.dungeon.prototype.model.inventory.items;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.weight.Weight;

import static org.dungeon.prototype.util.GenerationUtil.calculateWeaponWeight;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Weapon extends Item {
    private WeaponAttributes attributes;

    private Integer attack;
    private Double criticalHitChance;
    private Double criticalHitMultiplier;
    private Double chanceToMiss;
    private Double chanceToKnockOut;
    private Boolean isCompleteDragonBone;
    @Override
    public ItemType getItemType() {
        return ItemType.WEAPON;
    }
    @Override
    public Weight getWeight() {
        return calculateWeaponWeight(attack, criticalHitChance,
                criticalHitMultiplier, chanceToKnockOut,
                chanceToMiss, magicType)
                .add(effects.stream().map(Effect::getWeight).reduce(Weight::add)
                        .orElse(new Weight()));
    }

    public Weapon(Weapon weapon) {
        super(weapon);
        this.attributes = weapon.getAttributes();
        this.attack = weapon.getAttack();
        this.criticalHitChance = weapon.getCriticalHitChance();
        this.criticalHitMultiplier = weapon.getCriticalHitMultiplier();
        this.chanceToMiss = weapon.getChanceToMiss();
        this.chanceToKnockOut = weapon.getChanceToKnockOut();
        this.isCompleteDragonBone = weapon.getIsCompleteDragonBone();
    }
}
