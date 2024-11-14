package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.level.Level;
import org.dungeon.prototype.model.document.level.LevelDocument;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {RoomMapper.class, MonsterMapper.class, PointMapper.class})
public interface LevelMapper {
    LevelMapper INSTANCE = Mappers.getMapper(LevelMapper.class);

    static LevelMapper getInstance() {
        return INSTANCE;
    }

    LevelDocument mapToDocument(Level level);

    Level mapToLevel(LevelDocument document);
}
