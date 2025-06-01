package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import org.dungeon.prototype.model.document.item.EffectDocument;
import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.effect.Effect;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.inventory.items.Usable;
import org.dungeon.prototype.model.inventory.items.Weapon;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            @Mapping(target = "weightAbs", expression = "java(usable.getWeight().toVector().getNorm())")
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
            @Mapping(target = "weightAbs", expression = "java(wearable.getWeight().toVector().getNorm())")
    })
    ItemDocument mapWearableToDocument(Wearable wearable);

    @Mappings({
            @Mapping(target = "armor", ignore = true),
            @Mapping(target = "chanceToDodge", ignore = true),
            @Mapping(target = "amount", ignore = true),
            @Mapping(target = "weightAbs", expression = "java(weapon.getWeight().toVector().getNorm())")
    })
    ItemDocument mapWeaponToDocument(Weapon weapon);

    @Mappings({
            @Mapping(target = "effects", qualifiedByName = "mapEffects"),
            @Mapping(target = "weight", ignore = true),
            @Mapping(target = "buyingPrice", ignore = true),
            @Mapping(target = "sellingPrice", ignore = true),
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes) document.getAttributes())")
    })
    Wearable mapToWearable(ItemDocument document);

    @Mappings({
            @Mapping(target = "effects", qualifiedByName = "mapEffects"),
            @Mapping(target = "weight", ignore = true),
            @Mapping(target = "buyingPrice", ignore = true),
            @Mapping(target = "sellingPrice", ignore = true),
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes) document.getAttributes())")
    })
    Weapon mapToWeapon(ItemDocument document);

    @Mappings({
            @Mapping(target = "effects", qualifiedByName = "mapEffects"),
            @Mapping(target = "buyingPrice", ignore = true),
            @Mapping(target = "sellingPrice", ignore = true),
            @Mapping(target = "attributes", expression = "java((org.dungeon.prototype.model.inventory.attributes.usable.UsableAttributes) document.getAttributes())"),
    })
    Usable mapToUsable(ItemDocument document);

    @Named("mapEffects")
    default List<Effect> mapEffects(List<EffectDocument> documents) {
        return documents.stream()
                .map(EffectMapper.INSTANCE::mapToEffect)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
