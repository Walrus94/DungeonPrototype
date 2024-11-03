package org.dungeon.prototype.model.level;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.level.ui.LevelMap;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
@NoArgsConstructor
public class Level {

    private Long chatId;
    private Integer number;
    private Room start;
    private Room end;
    private GridSection[][] grid;
    private LevelMap levelMap;
    private int maxLength;
    private int minLength;
    private Map<Point, Room> roomsMap = new HashMap<>();

    public Room getRoomByCoordinates(Point currentPoint) {
        return roomsMap.get(currentPoint);
    }

    public void updateRoomType(Point point, RoomType type) {
        grid[point.getX()][point.getY()].setEmoji(getIcon(Optional.of(type)));
    }
}
