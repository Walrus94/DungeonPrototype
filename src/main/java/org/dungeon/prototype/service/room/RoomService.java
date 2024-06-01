package org.dungeon.prototype.service.room;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoomService {
    @Autowired
    RoomRepository roomRepository;

    public Room getRoomByIdAndChatId(Long chatId, String id) {
        return roomRepository.findByChatIdAndId(chatId, id);
    }

    public Room saveOrUpdateRoom(Room room) {
        return roomRepository.save(room);
    }
}
