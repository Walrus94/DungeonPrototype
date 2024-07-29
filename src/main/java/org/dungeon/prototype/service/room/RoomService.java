package org.dungeon.prototype.service.room;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.repository.RoomContentRepository;
import org.dungeon.prototype.repository.RoomRepository;
import org.dungeon.prototype.repository.converters.mapstruct.RoomContentMapper;
import org.dungeon.prototype.repository.converters.mapstruct.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component
public class RoomService {
    @Autowired
    RoomRepository roomRepository;

    @Autowired
    RoomContentRepository roomContentRepository;

    public Room getRoomByIdAndChatId(Long chatId, String id) {
        val roomDocument = roomRepository.findByChatIdAndId(chatId, id);
        return RoomMapper.INSTANCE.mapToRoom(roomDocument);
    }

    public Room saveOrUpdateRoom(Room room) {
        val roomDocument = RoomMapper.INSTANCE.mapToDocument(room);
        if (nonNull(roomDocument.getRoomContent()) && isNull(roomDocument.getRoomContent().getId())) {
            roomContentRepository.save(roomDocument.getRoomContent());
        }
        val savedRoomDocument = roomRepository.save(roomDocument);
        return RoomMapper.INSTANCE.mapToRoom(savedRoomDocument);
    }

    public RoomContent saveOrUpdateRoomContent(RoomContent roomContent) {
        val roomContentDocument = RoomContentMapper.INSTANCE.mapToRoomContentDocument(roomContent);
        val savedRoomContentDocument = roomContentRepository.save(roomContentDocument);
        return RoomContentMapper.INSTANCE.mapToRoomContent(savedRoomContentDocument);
    }
}
