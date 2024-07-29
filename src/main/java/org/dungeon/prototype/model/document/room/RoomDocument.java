package org.dungeon.prototype.model.document.room;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.EnumMap;

@Data
@NoArgsConstructor
@Document(collection = "rooms")
public class RoomDocument {
    @Id
    private String id;
    private Long chatId;
    private EnumMap<Direction, Boolean> adjacentRooms;
    @DBRef
    private RoomContentDocument roomContent;
    private Point point;
}
