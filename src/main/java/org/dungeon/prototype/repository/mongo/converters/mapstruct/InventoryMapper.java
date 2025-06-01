package org.dungeon.prototype.repository.mongo.converters.mapstruct;

import org.dungeon.prototype.model.document.item.ItemDocument;
import org.dungeon.prototype.model.document.player.InventoryDocument;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Mapper(uses = {ItemMapper.class, WeightMapper.class})
public interface InventoryMapper {
    InventoryMapper INSTANCE = Mappers.getMapper(InventoryMapper.class);

    InventoryDocument mapToDocument(Inventory inventory);

    @Mappings({
            @Mapping(source = "items", target = "items", qualifiedByName = "mapItems"),
            @Mapping(target = "maxItems", ignore = true),
            @Mapping(target = "armorItems", ignore = true),
            @Mapping(target = "weapons", ignore = true)
    })
    Inventory mapToEntity(InventoryDocument document);

    @Named("mapItems")
    default List<Item> mapItems(List<ItemDocument> documents) {
        if (isNull(documents) || documents.isEmpty()) {
            return new ArrayList<>();
        }
        return documents.stream()
                .filter(Objects::nonNull)
                .map(document -> switch (document.getItemType()) {
                    case WEAPON -> ItemMapper.INSTANCE.mapToWeapon(document);
                    case WEARABLE -> ItemMapper.INSTANCE.mapToWearable(document);
                    case USABLE -> ItemMapper.INSTANCE.mapToUsable(document);
                }).collect(Collectors.toCollection(ArrayList::new));
    }
}
