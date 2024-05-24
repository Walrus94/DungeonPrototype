package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.Room;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.model.ui.level.WalkerIterator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.dungeon.prototype.util.LevelUtil.Direction.E;
import static org.dungeon.prototype.util.LevelUtil.Direction.N;
import static org.dungeon.prototype.util.LevelUtil.Direction.S;
import static org.dungeon.prototype.util.LevelUtil.Direction.W;

@UtilityClass
public class LevelUtil {


    public enum Direction {
        N, E, S, W
    }
    private static final Integer LEVEL_ONE_GRID_SIZE = 10;
    private static final Integer GRID_SIZE_INCREMENT = 1;
    private static final Integer INCREMENT_STEP = 10;
    //TODO adjust according to level depth
    private static final Double MONSTER_RATIO = 50.0;
    private static final Double TREASURE_RATIO = 30.0;
    private static final Double ROOMS_RATIO = 0.6;
    private static final Double MAX_LENGTH_RATIO = 0.4;
    private static final Double MIN_LENGTH_RATIO = 0.2;
    private static final Integer MIN_LENGTH = 2;
    private static final Double DEAD_ENDS_RATIO = 0.1;
    public static Room buildRoom(Point nextPoint, Room.Type type) {
        return Room.builder()
                .type(type)
                .point(nextPoint)
                .build();
    }

    public static List<Direction> getAvailableDirections(Direction oldDirection) {
        if (oldDirection == null) {
            return randomizeDirections(Arrays.asList(Direction.values()));
        }
        return randomizeDirections(
                Arrays.stream(Direction.values())
                        .filter(dir -> !dir.equals(oldDirection) && ! dir.equals(getOppositeDirection(oldDirection)))
                        .collect(Collectors.toList()));
    }

    public static List<Direction> randomizeDirections(List<Direction> directions) {
        Collections.shuffle(directions);
        return directions;
    }

    public static Direction getOppositeDirection(Direction direction) {
        return switch (direction) {
            case N -> S;
            case E -> W;
            case S -> N;
            case W -> E;
        };
    }

    public static Direction turnLeft(Direction direction) {
        return switch (direction) {
            case N -> W;
            case W -> S;
            case S -> E;
            case E -> N;
        };
    }

    public static Direction turnRight(Direction direction) {
        return switch (direction) {
            case N -> E;
            case E -> S;
            case S -> W;
            case W -> N;
        };
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

    public static Integer calculateMinLength(Integer gridSize) {
        return (int) (gridSize * MIN_LENGTH_RATIO) < MIN_LENGTH ? MIN_LENGTH :
                (int) (gridSize * MIN_LENGTH_RATIO);
    }

    public static boolean isPossibleCrossroad(WalkerIterator walkerIterator, int minLength, int gridLength) {
        return (walkerIterator.getPathFromStart() > 0) &&
                (walkerIterator.getCurrentPoint().getPoint().getX() < gridLength - minLength) &&
                (walkerIterator.getCurrentPoint().getPoint().getX() > minLength) &&
                (walkerIterator.getCurrentPoint().getPoint().getY() < gridLength - minLength) &&
                (walkerIterator.getCurrentPoint().getPoint().getY() > minLength);
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
        for (int y = map.length - 1; y >= 0; y--) {
            for (int x = 0; x < map.length; x++) {
                result.append(map[x][y].getEmoji());
            }
            result.append("\n");
        }
        return result.toString();
    }

    public static String printMap(GridSection[][] grid, LevelMap levelMap, Point position) {
        StringBuilder result = new StringBuilder();
        for (int y = levelMap.getMaxY(); y >= levelMap.getMinY(); y--) {
            for (int x = levelMap.getMinX(); x <= levelMap.getMaxX(); x++) {
                if (levelMap.isContainsRoom(x, y)) {
                    if (x == position.getX() && y == position.getY()) {
                        result.append(getPointerIcon());
                    } else {
                        result.append(grid[x][y].getEmoji());
                    }
                } else {
                    result.append("\uD83D\uDFEB");
                }
            }
            result.append("\n");
        }
        return result.toString();
    }

    public static String printMap(GridSection[][] map, Point position) {
        StringBuilder result = new StringBuilder();
        for (int y = map.length - 1; y >= 0; y--) {
            for (int x = 0; x < map.length; x++) {
                if (x == position.getX() && y == position.getY()) {
                    result.append(getPointerIcon());
                } else {
                    result.append(map[x][y].getEmoji());
                }
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
            case MONSTER_KILLED -> "\uD83D\uDC80";
            case TREASURE -> "\uD83D\uDCB0";
            case TREASURE_LOOTED -> "\uD83D\uDDD1";
            case END -> "\uD83C\uDFC1";
        }).orElse("\uD83D\uDFEB");
    }

    public static String getPointerIcon() {
        return "\uD83D\uDD34";
    }
}
