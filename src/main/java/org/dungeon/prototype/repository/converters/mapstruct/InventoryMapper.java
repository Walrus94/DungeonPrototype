package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.document.player.InventoryDocument;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(uses = {ArmorSetMapper.class, WeaponSetMapper.class, ItemMapper.class})
public interface InventoryMapper {
    InventoryMapper INSTANCE = Mappers.getMapper(InventoryMapper.class);

    InventoryDocument mapToDocument(Inventory inventory);

    @Mappings({
            @Mapping(source = "items", target = "items", qualifiedByName = "mapItems"),
            @Mapping(target = "maxItems", ignore = true)
    })
    Inventory mapToEntity(InventoryDocument document);

    @Named("mapItems")
    default List<Item> mapItems(List<ItemDocument> documents) {
        return documents.stream().map(document -> switch (document.getItemType()) {
            case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(document);
            case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(document);
        }).collect(Collectors.toList());
    }
}
