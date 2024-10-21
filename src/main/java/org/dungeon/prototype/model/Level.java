package org.dungeon.prototype.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private Set<GridSection> deadEnds = new HashSet<>();
    private final Map<Point, RoomsSegment> deadEndToSegmentMap = new HashMap<>();
    private int maxLength;
    private int minLength;
    private Map<Point, Room> roomsMap = new HashMap<>();

    public Room getRoomByCoordinates(Point currentPoint) {
        return roomsMap.get(currentPoint);
    }

    public void updateRoomType(Point point, RoomType type) {
        grid[point.getX()][point.getY()].setEmoji(getIcon(Optional.of(type)));
    }

    public void removeDeadEnd(GridSection deadEnd) {
        this.deadEnds.remove(deadEnd);
        this.deadEndToSegmentMap.remove(deadEnd.getPoint());
    }
}
