package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.document.level.LevelDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {RoomMapper.class, MonsterMapper.class, PointMapper.class})
public interface LevelMapper {
    LevelMapper INSTANCE = Mappers.getMapper(LevelMapper.class);

    static LevelMapper getInstance() {
        return INSTANCE;
    }

    LevelDocument mapToDocument(Level level);

    @Mappings({
            @Mapping(target = "deadEnds", ignore = true),
            @Mapping(target = "maxLength", ignore = true),
            @Mapping(target = "minLength", ignore = true),
            @Mapping(target = "deadEndToSegmentMap", ignore = true),
    })
    Level mapToLevel(LevelDocument document);
}
