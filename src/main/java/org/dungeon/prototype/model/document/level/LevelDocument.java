package org.dungeon.prototype.model.document.level;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.document.room.RoomDocument;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@Document(collection = "levels")
public class LevelDocument {
    @Id
    private Long chatId;
    private Integer number;
    @DBRef
    private RoomDocument start;
    @DBRef
    private RoomDocument end;
    private GridSection[][] grid;
    private LevelMap levelMap;
    @DBRef
    private Map<String, RoomDocument> roomsMap;
}
