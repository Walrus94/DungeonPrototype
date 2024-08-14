package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.document.item.ItemSpecs;
import org.dungeon.prototype.model.document.item.specs.UsableSpecs;
import org.dungeon.prototype.model.document.item.specs.WeaponSpecs;
import org.dungeon.prototype.model.document.item.specs.WearableSpecs;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.Usable;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EffectMapper.class)
public interface ItemMapper {
    ItemMapper INSTANCE = Mappers.getMapper(ItemMapper.class);

    @Mappings({
            @Mapping(target = "specs", source = "item", qualifiedByName = "mapToSpecs")
    })
    ItemDocument mapToDocument(Item item);

    @Mappings({
            @Mapping(target = "armor", expression = "java(((org.dungeon.prototype.model.document.item.specs.WearableSpecs) document.getSpecs()).getArmor())"),
            @Mapping(target = "chanceToDodge", expression = "java(((org.dungeon.prototype.model.document.item.specs.WearableSpecs) document.getSpecs()).getChanceToDodge())"),
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes) document.getAttributes())")
    })
    Wearable mapToWearable(ItemDocument document);

    @Mappings({
            @Mapping(target = "attack", expression = "java(((org.dungeon.prototype.model.document.item.specs.WeaponSpecs) document.getSpecs()).getAttack())"),
            @Mapping(target = "criticalHitChance", expression = "java(((org.dungeon.prototype.model.document.item.specs.WeaponSpecs) document.getSpecs()).getCriticalHitChance())"),
            @Mapping(target = "chanceToMiss", expression = "java(((org.dungeon.prototype.model.document.item.specs.WeaponSpecs) document.getSpecs()).getChanceToMiss())"),
            @Mapping(target = "chanceToKnockOut", expression = "java(((org.dungeon.prototype.model.document.item.specs.WeaponSpecs) document.getSpecs()).getChanceToKnockOut())"),
            @Mapping(target = "completeDragonBone", expression = "java(((org.dungeon.prototype.model.document.item.specs.WeaponSpecs) document.getSpecs()).isCompleteDragonBone())"),
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes) document.getAttributes())")
    })
    Weapon mapToWeapon(ItemDocument document);

    @Mappings({
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.usable.UsableAttributes) document.getAttributes())"),
            @Mapping(target = "amount",  expression = "java(((org.dungeon.prototype.model.document.item.specs.UsableSpecs) document.getSpecs()).getAmount())")
    })
    Usable mapToUsable(ItemDocument document);

    @Named("mapToSpecs")
    default ItemSpecs mapToSpecs(Item item){
        if (item instanceof Weapon weapon) {
            WeaponSpecs specs = new WeaponSpecs();
            specs.setAttack(weapon.getAttack());
            specs.setChanceToKnockOut(weapon.getChanceToKnockOut());
            specs.setCriticalHitChance(weapon.getCriticalHitChance());
            specs.setCompleteDragonBone(weapon.isCompleteDragonBone());
            specs.setChanceToMiss(weapon.getChanceToMiss());
            return specs;
        } else if (item instanceof Wearable wearable) {
            WearableSpecs wearableSpecs = new WearableSpecs();
            wearableSpecs.setArmor(wearable.getArmor());
            wearableSpecs.setChanceToDodge(wearable.getChanceToDodge());
            return wearableSpecs;
        } else if (item instanceof Usable usable) {
            UsableSpecs usableSpecs = new UsableSpecs();
            usableSpecs.setAmount(usable.getAmount());
            return usableSpecs;
        }
        return null;
    }
}
