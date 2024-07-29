package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.player.WeaponSetDocument;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = ItemMapper.class)
public interface WeaponSetMapper {
    WeaponSetMapper INSTANCE = Mappers.getMapper(WeaponSetMapper.class);

    WeaponSetDocument mapToDocument(WeaponSet weaponSet);
    @Mapping(target = "weapons", ignore = true)
    WeaponSet mapToEntity(WeaponSetDocument weaponSetDocument);
}
