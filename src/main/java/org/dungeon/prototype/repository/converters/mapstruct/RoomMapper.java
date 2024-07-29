package org.dungeon.prototype.repository.converters.mapstruct;

import org.dungeon.prototype.model.document.room.RoomDocument;
import org.dungeon.prototype.model.room.Room;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = RoomContentMapper.class)
public interface RoomMapper {
    RoomMapper INSTANCE = Mappers.getMapper(RoomMapper.class);

    RoomDocument mapToDocument(Room room);

    Room mapToRoom(RoomDocument document);

}
