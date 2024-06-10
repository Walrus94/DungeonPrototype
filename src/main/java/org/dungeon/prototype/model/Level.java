package org.dungeon.prototype.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.service.room.WalkerDistributeIterator;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
@Slf4j
@NoArgsConstructor
@Document(collection = "levels")
public class Level {

    @Transient
    private static final Integer MAX_CROSSROAD_RECURSION_LEVEL = 1;

    @Id
    private Long chatId;
    private Integer number;
    private Room start;
    private Room end;
    private GridSection[][] grid;
    private LevelMap levelMap;
    @Transient
    private Set<GridSection> deadEnds = new HashSet<>();
    @Transient
    private final Map<Point, RoomsSegment> deadEndToSegmentMap = new HashMap<>();
    @Transient
    private int maxLength;
    @Transient
    private int minLength;
    private Map<Point, Room> roomsMap = new HashMap<>(); //TODO: consider different map implementation

    @Transient
    private final Queue<WalkerDistributeIterator> distributeIterators = new LinkedList<>();

    public Room getRoomByCoordinates(Point currentPoint) {
        return roomsMap.get(currentPoint);
    }

    public void updateRoomType(Point point, RoomType type) {
        getRoomByCoordinates(point).setRoomContent(new EmptyRoom(type));
        grid[point.getX()][point.getY()].setEmoji(getIcon(Optional.of(type)));
    }

    public void removeDeadEnd(GridSection deadEnd) {
        this.deadEnds.remove(deadEnd);
    }
}
