package org.dungeon.prototype.model.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.room.content.NormalRoom;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.EnumMap;


@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rooms")
public class Room {

    public Room(Point point, Long chatId, RoomContent roomContent) {
        this.point = point;
        this.chatId = chatId;
        this.roomContent = roomContent;
    }

    public Room(Point point, Long chatId) {
        this.point = point;
        this.chatId = chatId;
        this.roomContent = new NormalRoom();
    }
    @Id
    private String id;
    private Long chatId;
    private EnumMap<Direction, Boolean> adjacentRooms = getDefaultEnumMap();

    private EnumMap<Direction, Boolean> getDefaultEnumMap() {
        EnumMap<Direction, Boolean> map = new EnumMap<>(Direction.class);
        map.put(Direction.N, false);
        map.put(Direction.E, false);
        map.put(Direction.S, false);
        map.put(Direction.W, false);
        return map;
    }

    private RoomContent roomContent;
    private Point point;

    public void addAdjacentRoom(Direction direction) {
        adjacentRooms.put(direction, true);

        log.debug("Added adjacent room to {} in {} direction", point, direction);
    }

    @Override
    public String toString() {
        return "Room [type=" + roomContent.getRoomType() + ", point=" + point + "]";
    }
}
