package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.service.level.WalkerBuilderIterator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.math3.util.FastMath.toIntExact;
import static org.dungeon.prototype.model.Direction.E;
import static org.dungeon.prototype.model.Direction.N;
import static org.dungeon.prototype.model.Direction.S;
import static org.dungeon.prototype.model.Direction.W;
import static org.dungeon.prototype.util.RandomUtil.flipAdjustedCoin;

@Slf4j
@UtilityClass
public class LevelUtil {
    public static RoomType getMonsterKilledRoomType(RoomType roomType) {
        return switch (roomType) {
            case WEREWOLF -> RoomType.WEREWOLF_KILLED;
            case VAMPIRE -> RoomType.VAMPIRE_KILLED;
            case SWAMP_BEAST -> RoomType.SWAMP_BEAST_KILLED;
            case DRAGON -> RoomType.DRAGON_KILLED;
            case ZOMBIE -> RoomType.ZOMBIE_KILLED;
            default -> throw new IllegalStateException("Unexpected value: " + roomType);
        };
    }
    public static Optional<Direction> getRandomValidDirection(WalkerBuilderIterator walkerBuilderIterator, Level level) {
        val grid = level.getGrid();
        val visitedRooms = level.getRoomsMap().keySet();
        val gridSize = grid.length;
        val oldDirection = walkerBuilderIterator.getDirection();
        val currentPoint = walkerBuilderIterator.getCurrentPoint();
        val gridSections = getGridSections(grid, visitedRooms);
        val validDirections = Arrays.stream(Direction.values())
                .filter(dir -> !dir.equals(oldDirection) && ! dir.equals(getOppositeDirection(oldDirection)))
                .filter(direction -> calculateMaxLengthInDirection(grid, currentPoint, direction) >= level.getMinLength())
                .collect(Collectors.toList());
        return getRandomDirection(validDirections, currentPoint.getPoint(), gridSections, gridSize);
    }

    public static Optional<Direction> getRandomDirection(List<Direction> directions, Point currentPoint, Set<GridSection> visitedRooms, int gridSize) {
        double s, n, w, e;
        log.debug("Randomizing direction...");
        var nCount = toIntExact(visitedRooms.stream()
                .map(GridSection::getPoint)
                .filter(point -> point.getY() > currentPoint.getY())
                .count());
        var sCount = toIntExact(visitedRooms.stream()
                .map(GridSection::getPoint)
                .filter(point -> point.getY() < currentPoint.getY())
                .count());
        var wCount = toIntExact(visitedRooms.stream()
                .map(GridSection::getPoint)
                .filter(point -> point.getX() > currentPoint.getX())
                .count());
        var eCount = toIntExact(visitedRooms.stream()
                .map(GridSection::getPoint)
                .filter(point -> point.getX() < currentPoint.getX())
                .count());
        if (directions.contains(S)) {
            s = nCount == 0 && sCount == 0 ?
                    currentPoint.getY().doubleValue() / gridSize :
                    (double) nCount / visitedRooms.size();
        } else {
            s = 0.0;
        }
        n = directions.contains(N) ? 1.0 - s : 0.0;

        if (directions.contains(W)) {
            w = eCount == 0 && wCount == 0 ?
                    currentPoint.getX().doubleValue() / gridSize :
                    (double) eCount / visitedRooms.size();
        } else {
            w = 0.0;
        }
        e = directions.contains(E) ? 1.0 - w : 0.0;
        if (s + n + w + e > 0.0) {
            return Optional.of(RandomUtil.getRandomDirection(List.of(
                    Pair.create(S, s),
                    Pair.create(N, n),
                    Pair.create(W, w),
                    Pair.create(E, e))));
        } else {
            return Optional.empty();
        }
    }

    public static Direction getOppositeDirection(Direction direction) {
        if (Objects.isNull(direction)) {
            return null;
        }
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

    public static Point getNextPointInDirection(Point point, Direction direction) {
        return switch (direction) {
            case N -> new Point(point.getX(), point.getY() + 1);
            case S -> new Point(point.getX(), point.getY() - 1);
            case W -> new Point(point.getX() - 1, point.getY());
            case E -> new Point(point.getX() + 1, point.getY());
        };
    }

    private static Set<GridSection> getGridSections(GridSection[][] grid, Set<Point> points) {
        return points.stream().map(point -> grid[point.getX()][point.getY()]).collect(Collectors.toSet());
    }

    public static boolean isCrossroad(WalkerBuilderIterator walkerBuilderIterator, Level level, int waitingWalkerBuilders) {
        val currentPoint = walkerBuilderIterator.getCurrentPoint();
        val oldDirection = walkerBuilderIterator.getDirection();
        val pathFromStart = walkerBuilderIterator.getPathFromStart();
        val grid = level.getGrid();
        val levelSize = level.getRoomsMap().size();
        val minLength = level.getMinLength();
        val isCrossroad = flipAdjustedCoin(((double) (pathFromStart - waitingWalkerBuilders)) / (double) levelSize);
        return isCrossroad && Arrays.stream(Direction.values())
                .filter(direction -> !direction.equals(oldDirection) && !direction.equals(getOppositeDirection(oldDirection)))
                .filter(direction -> calculateMaxLengthInDirection(grid, currentPoint, direction) > minLength)
                .count() > 1;
    }



    public static Integer calculateMaxLengthInDirection(GridSection[][] grid, GridSection startSection, Direction direction) {
        log.debug("Calculating max length in {} direction...", direction);
        GridSection currentSection = null;
        var path = 0;
        while (currentSection == null || !currentSection.getVisited()) {
            if (currentSection == null) {
                currentSection = startSection;
            }
            val nextPoint = getNextPointInDirection(currentSection.getPoint(), direction);
            if (isPointOnGrid(nextPoint, grid.length)) {
                val nextSection = grid[nextPoint.getX()][nextPoint.getY()];
                if (nextSection.getVisited()) {
                    log.debug("Visited room reached, returning max length of {}", path);
                    return path;
                }
                val nextNextPoint = getNextPointInDirection(nextPoint, direction);
                if (isPointOnGrid(nextNextPoint, grid.length)) {
                    val nextNextSection = grid[nextNextPoint.getX()][nextNextPoint.getY()];
                    if (nextNextSection.getVisited()) {
                        log.debug("Visited room is one step away, returning max length of {}", path);
                        return path;
                    }
                    currentSection = nextSection;
                    log.debug("Adding {} step...", currentSection.getPoint());
                    path++;
                    log.debug("Current path length: {}", path);
                } else {
                    log.debug("Edge of map is one step away, returning max length of {}", path);
                    return path;
                }
            } else {
                log.debug("Edge of map reached, returning max length of {}", path);
                return path;
            }
        }
        return path;
    }

    private static boolean isPointOnGrid(Point nextPoint, int gridSize) {
        return nextPoint.getY() < gridSize && nextPoint.getY() > -1 &&
                nextPoint.getX() < gridSize && nextPoint.getX() > -1;
    }

    public static RoomsSegment getMainSegment(Level level) {
        val startSection = level.getGrid()[level.getStart().getPoint().getX()][level.getStart().getPoint().getY()];
        val endSection = level.getGrid()[level.getEnd().getPoint().getX()][level.getEnd().getPoint().getY()];
        return new RoomsSegment(startSection, endSection);
    }


    public static String printMap(GridSection[][] grid, LevelMap levelMap, Point position, Direction direction) {
        StringBuilder result = new StringBuilder();
        for (int y = levelMap.getMaxY(); y >= levelMap.getMinY(); y--) {
            for (int x = levelMap.getMinX(); x <= levelMap.getMaxX(); x++) {
                if (levelMap.isContainsRoom(x, y)) {
                    if (x == position.getX() && y == position.getY()) {
                        result.append(getPointerIcon(direction));
                    } else {
                        result.append(grid[x][y].getEmoji());
                    }
                } else {
                    result.append(getBlankIcon());
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

    public static String getIcon(Optional<RoomType> roomType) {
        return roomType.map(type -> switch (type) {
            case NORMAL -> "\uD83D\uDFE7";
            case START -> "\uD83D\uDEAA";
            case WEREWOLF, VAMPIRE, SWAMP_BEAST, DRAGON, ZOMBIE -> "\uD83D\uDC7E";
            case WEREWOLF_KILLED, VAMPIRE_KILLED, SWAMP_BEAST_KILLED, DRAGON_KILLED, ZOMBIE_KILLED -> "\uD83D\uDC80";
            case TREASURE -> "\uD83D\uDCB0";
            case TREASURE_LOOTED -> "\uD83D\uDDD1";
            case MANA_SHRINE -> "\uD83D\uDD2E";
            case HEALTH_SHRINE -> "\uD83C\uDFE5";
            case ANVIL -> "\uD83D\uDD28";
            case SHRINE_DRAINED -> "\uD83E\uDEA6";
            case END -> "\uD83C\uDFC1";
            case MERCHANT -> "\uD83E\uDDD9";
        }).orElseGet(LevelUtil::getBlankIcon);
    }

    public static String getPointerIcon(Direction direction) {
        String icon = switch (direction) {
            case N -> "⏫";
            case E -> "⏩";
            case S -> "⏬";
            case W -> "⏪";
        };
        byte[] bytes = icon.getBytes(StandardCharsets.UTF_8);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String getBlankIcon() {
        return "\uD83D\uDFEB";
    }

    //FOR DEBUGGING

    public static String printMap(GridSection[][] map) {
        StringBuilder result = new StringBuilder();
        for (int y = map.length - 1; y >= 0; y--) {
            for (int x = 0; x < map.length; x++) {
                if (map[x][y].getDeadEnd() || map[x][y].getCrossroad()) {
                    if (map[x][y].getDeadEnd() && !map[x][y].getCrossroad()) {
                        result.append(getIcon(Optional.of(RoomType.END)).equals(map[x][y].getEmoji()) ?
                                map[x][y].getEmoji() :
                                getDeadEndIcon());
                    }
                    if (!map[x][y].getDeadEnd() && map[x][y].getCrossroad()) {
                        result.append(getCrossroadIcon());
                    }
                    if (map[x][y].getDeadEnd() && map[x][y].getCrossroad()) {
                        log.warn("Warning! Point (x:{}, y:{}) marked as BOTH crossroad and dead end!", x, y);
                        result.append(getCrossroadAndDeadEndWarningIcon());
                    }
                } else {
                    result.append(map[x][y].getEmoji());
                }
            }
            result.append("\n");
        }
        return result.toString();
    }

    public static String getDeadEndIcon() {
        return "\uD83D\uDED1";
    }

    public static String getCrossroadIcon() {
        return "\uD83D\uDD04";
    }

    public static String getCrossroadAndDeadEndWarningIcon() {
        return "\uD83D\uDEAB";
    }
}
