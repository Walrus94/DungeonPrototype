package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.Usable;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EffectMapper.class)
public interface ItemMapper {
    ItemMapper INSTANCE = Mappers.getMapper(ItemMapper.class);

    default ItemDocument mapToDocument(Item item) {
        return switch (item.getItemType()) {
            case WEAPON -> mapWeaponToDocument((Weapon) item);
            case WEARABLE -> mapWearableToDocument((Wearable) item);
            case USABLE -> mapUsableToDocument((Usable) item);
        };
    }

    @Mappings({
            @Mapping(target = "attack", ignore = true),
            @Mapping(target = "criticalHitChance", ignore = true),
            @Mapping(target = "criticalHitMultiplier", ignore = true),
            @Mapping(target = "chanceToMiss", ignore = true),
            @Mapping(target = "chanceToKnockOut", ignore = true),
            @Mapping(target = "isCompleteDragonBone", ignore = true),
            @Mapping(target = "armor", ignore = true),
            @Mapping(target = "chanceToDodge", ignore = true),
    })
    ItemDocument mapUsableToDocument(Usable usable);

    @Mappings({
            @Mapping(target = "attack", ignore = true),
            @Mapping(target = "criticalHitChance", ignore = true),
            @Mapping(target = "criticalHitMultiplier", ignore = true),
            @Mapping(target = "chanceToMiss", ignore = true),
            @Mapping(target = "chanceToKnockOut", ignore = true),
            @Mapping(target = "isCompleteDragonBone", ignore = true),
            @Mapping(target = "amount", ignore = true),
    })
    ItemDocument mapWearableToDocument(Wearable wearable);

    @Mappings({
            @Mapping(target = "armor", ignore = true),
            @Mapping(target = "chanceToDodge", ignore = true),
            @Mapping(target = "amount", ignore = true)
    })
    ItemDocument mapWeaponToDocument(Weapon weapon);

    @Mappings({
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes) document.getAttributes())")
    })
    Wearable mapToWearable(ItemDocument document);

    @Mappings({
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes) document.getAttributes())")
    })
    Weapon mapToWeapon(ItemDocument document);

    @Mappings({
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.usable.UsableAttributes) document.getAttributes())"),
    })
    Usable mapToUsable(ItemDocument document);
}
