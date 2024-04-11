package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.Room;
import org.dungeon.prototype.model.ui.level.GridSection;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

@UtilityClass
public class LevelUtil {
    private static final Integer LEVEL_ONE_GRID_SIZE = 10;
    private static final Integer GRID_SIZE_INCREMENT = 1;
    private static final Integer INCREMENT_STEP = 10;
    private static final Integer MAX_RECURSION_LEVEL = 1;

    //TODO adjust according to level depth
    private static final Double MONSTER_RATIO = 30.0;
    private static final Double TREASURE_RATIO = 20.0;
    private static final Double ROOMS_RATIO = 0.4;
    private static final Double MAX_LENGTH_RATIO = 0.7;
    private static final Double DEAD_ENDS_RATIO = 0.1;

    public static Room buildRoom(Room previousRoom, Point nextPoint, Room.Type type) {
        return Room.builder()
                .entrance(previousRoom)
                .type(type)
                .point(nextPoint)
                .build();
    }

    public static int calculateDeadEndsCount(int roomTotal) {
        return (int) (roomTotal * DEAD_ENDS_RATIO);
    }

    public static int calculateAmountOfTreasures(int roomTotal) {
        return (int) (roomTotal * TREASURE_RATIO / 100);
    }

    public static int calculateAmountOfMonsters(int roomTotal) {
        return (int) (roomTotal * MONSTER_RATIO / 100);
    }

    public static Integer calculateMaxLength(Integer gridSize) {
        return (int) (gridSize * MAX_LENGTH_RATIO);
    }

    public static NavigableMap<Double, Room.Type> getRoomTypeWeights() {
        NavigableMap<Double, Room.Type> roomTypesWeights = new TreeMap<>();
        roomTypesWeights.put(MONSTER_RATIO, Room.Type.MONSTER);
        roomTypesWeights.put(TREASURE_RATIO, Room.Type.TREASURE);
        roomTypesWeights.put(Double.sum(100.0, -Double.sum(MONSTER_RATIO, TREASURE_RATIO)), Room.Type.NORMAL);
        return roomTypesWeights;
    }


    public static String printMap(GridSection[][] map) {
        StringBuilder result = new StringBuilder();
        for (int x = map.length - 1; x >= 0; x--) {
            GridSection[] row = map[x];
            for (GridSection s : row) {
                result.append(s.getEmoji());
            }
            result.append("\n");
        }
        return result.toString();
    }

    public static GridSection[][] generateEmptyMapGrid(Integer gridSize) {
        GridSection[][] grid = new GridSection[gridSize][gridSize];
        for (int x = 0; x < gridSize; x++) {
            GridSection[] row = new GridSection[gridSize];
            for (int y = 0; y < gridSize; y++) {
                row[y] = new GridSection(x, y);
            }
            grid[x] = row;
        }
        return grid;
    }

    public static String buildLevelMap(Level level) {
        String[][] resultingMap = new String[level.getGrid().length][level.getGrid().length];
        for (int x = 0; x < resultingMap.length; x++) {
            for (int y = 0; y < resultingMap.length; y++) {
                resultingMap[x][y] = getIcon(Optional.empty());
            }
        }
        setLevelRoom(level.getStart(), resultingMap);

        StringBuilder stringBuilder = new StringBuilder();
        for (int x = 0; x < resultingMap.length; x++) {
            for (int y = 0; y < resultingMap.length; y++) {
                stringBuilder.append(resultingMap[x][y]);
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    private static void setLevelRoom(Room room, String[][] resultingMap) {
        if (room == null) {
            return;
        }
        val currentPoint = room.getPoint();
        resultingMap[currentPoint.getX()][currentPoint.getY()] = getIcon(Optional.ofNullable(room.getType()));
        setLevelRoom(room.getLeft(), resultingMap);
        setLevelRoom(room.getRight(), resultingMap);
        setLevelRoom(room.getMiddle(), resultingMap);
    }

    public static Integer calculateGridSize(Integer levelNumber) {
        val increments = (levelNumber - 1) / INCREMENT_STEP;
        return LEVEL_ONE_GRID_SIZE + increments * GRID_SIZE_INCREMENT;
    }

    public static int calculateAmountOfRooms(Integer gridSize) {
        return (int) (gridSize * gridSize * ROOMS_RATIO);
    }

    public static String getIcon(Optional<Room.Type> roomType) {
        return roomType.map(type -> switch (type) {
            case NORMAL -> "\uD83D\uDFE7";
            case START -> "\uD83D\uDEAA";
            case MONSTER -> "\uD83D\uDC7E";
            case TREASURE -> "\uD83D\uDCB0";
            case END -> "\uD83C\uDFC1";
        }).orElse("\uD83D\uDFEB");
    }
}
