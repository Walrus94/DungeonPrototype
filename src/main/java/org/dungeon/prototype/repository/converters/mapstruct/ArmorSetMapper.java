package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.player.ArmorSetDocument;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(uses = ItemMapper.class)
public interface ArmorSetMapper {
    ArmorSetMapper INSTANCE = Mappers.getMapper(ArmorSetMapper.class);

    ArmorSetDocument mapToDocument(ArmorSet armorSet);
    @Mappings({
            @Mapping(target = "attackTypeResistanceMap", ignore = true),
            @Mapping(target = "armorItems", ignore = true)
    })
    ArmorSet mapToEntity(ArmorSetDocument document);
}
