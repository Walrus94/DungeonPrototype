package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.monster.MonsterDocument;
import org.dungeon.prototype.model.monster.Monster;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EffectMapper.class, WeightMapper.class})
public interface MonsterMapper {
    MonsterMapper INSTANCE = Mappers.getMapper(MonsterMapper.class);

    MonsterDocument mapToDocument(Monster monster);

    @Mappings({
            @Mapping(target = "weight", ignore = true)
    })
    Monster mapToMonster(MonsterDocument document);
}
