package org.dungeon.prototype.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.level.generation.LevelGridCluster;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.level.ui.LevelMap;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.properties.CallbackType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.toIntExact;
import static org.dungeon.prototype.model.Direction.E;
import static org.dungeon.prototype.model.Direction.N;
import static org.dungeon.prototype.model.Direction.S;
import static org.dungeon.prototype.model.Direction.W;

@Slf4j
@UtilityClass
public class LevelUtil {
    public static RoomType getMonsterKilledRoomType(RoomType roomType) {
        return switch (roomType) {
            //TODO: investigate why KILLED type appears occasionally
            case WEREWOLF, WEREWOLF_KILLED -> RoomType.WEREWOLF_KILLED;
            case VAMPIRE, VAMPIRE_KILLED -> RoomType.VAMPIRE_KILLED;
            case SWAMP_BEAST, SWAMP_BEAST_KILLED -> RoomType.SWAMP_BEAST_KILLED;
            case DRAGON, DRAGON_KILLED -> RoomType.DRAGON_KILLED;
            case ZOMBIE, ZOMBIE_KILLED -> RoomType.ZOMBIE_KILLED;
            default -> throw new IllegalStateException("Unexpected value: " + roomType);
        };
    }

    public static Direction getDirection(Room room, Room previousRoom) {
        Point point = room.getPoint();
        Point previousPoint = previousRoom.getPoint();

        int x = point.getX().compareTo(previousPoint.getX());
        int y = point.getY().compareTo(previousPoint.getY());

        if (x == 0) {
            if (y > 0) {
                return N;
            }
            if (y < 0) {
                return S;
            }
            return null;
        } else {
            if (y != 0) {
                return null;
            }
            if (x > 0) {
                return E;
            }
            return W;
        }
    }

    public static void setMutualAdjacency(Room room, Room previousRoom) {
        log.debug("Setting mutual adjacency of {} and {}", room, previousRoom);
        if (room.getPoint().equals(previousRoom.getPoint())) {
            log.debug("Same rooms passed as arguments. unable to set adjacency");
            return;
        }
        var direction = getDirection(room, previousRoom);
        log.debug("Direction: {}", direction);
        if (nonNull(direction)) {//TODO: investigate NPE
            previousRoom.addAdjacentRoom(direction);
            room.addAdjacentRoom(getOppositeDirection(direction));
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

    public static Direction getDirectionSwitchByCallBackData(Direction direction, CallbackType callBackData) {
        return switch (callBackData) {
            case LEFT -> turnLeft(direction);
            case RIGHT -> turnRight(direction);
            case BACK -> getOppositeDirection(direction);
            default -> direction;
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

    //TODO: get rid of it
    public static String getErrorMessageByCallBackData(CallbackType callBackData) {
        return switch (callBackData) {
                case LEFT -> "Left door is locked!";
                case RIGHT -> "Right door is locked!";
                case FORWARD -> "Middle door is locked!";
                case BACK -> "Door on the back is locked!";
                default -> "Wrong callBack data!";
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

    /**
     * Selects random direction with probability depending on
     * already filled sections or, if none present, by length from grid border
     * @param directions available directions
     * @param currentPoint room from where direction selected
     * @param visitedRooms grid section already filled with rooms
     * @param gridSize size of level grid
     * @return present if random direction available
     */
    private static Optional<Direction> getRandomDirection(List<Direction> directions, Point currentPoint, Set<GridSection> visitedRooms, int gridSize) {
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

    public static boolean isPointOnGrid(Point point, int gridSize) {
        return point.getY() < gridSize && point.getY() > -1 &&
                point.getX() < gridSize && point.getX() > -1;
    }

    public static Set<GridSection> getAdjacentSections(Point currentPoint, GridSection[][] grid) {
        return Stream.of(new Point(currentPoint.getX() + 1, currentPoint.getY()),
                        new Point(currentPoint.getX() - 1, currentPoint.getY()),
                        new Point(currentPoint.getX(), currentPoint.getY() + 1),
                        new Point(currentPoint.getX(), currentPoint.getY() - 1))
                .unordered()
                .filter(point -> isPointOnGrid(point, grid.length))
                .map(point -> grid[point.getX()][point.getY()])
                .collect(Collectors.toSet());
    }

    public static Set<GridSection> getAdjacentSectionsInCluster(Point currentPoint, GridSection[][] grid, LevelGridCluster cluster) {
        log.debug("Getting adjacent sections for {} in cluster {}", currentPoint, cluster);
        return getAdjacentSections(currentPoint, grid).stream()
                .filter(section -> isPointInCluster(section.getPoint(), cluster))
                .collect(Collectors.toSet());
    }

    public static boolean isPointInCluster(Point point, LevelGridCluster cluster) {
        return cluster.getStartConnectionPoint().getX() <= point.getX() &&
                cluster.getStartConnectionPoint().getY() <= point.getY() &&
                cluster.getEndConnectionPoint().getX() >= point.getX() &&
                cluster.getEndConnectionPoint().getY() >= point.getY();
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

    public static String printMapGridToLogs(GridSection[][] map) {
        StringBuilder result = new StringBuilder();
        for (int y = map.length - 1; y >= 0; y--) {
            for (GridSection[] gridSections : map) {
                if (gridSections[y].isConnectionPoint()) {
                    result.append(getCrossroadIcon());
                } else if (gridSections[y].isDeadEnd()) {
                    if (gridSections[y].getStepsFromStart() > 9) {
                        result.append("{").append(gridSections[y].getStepsFromStart()).append("}");
                    } else {
                        result.append("{0").append(gridSections[y].getStepsFromStart()).append("}");
                    }
                } else {
                    if (gridSections[y].getStepsFromStart() < 0) {
                        result.append("[").append(gridSections[y].getStepsFromStart()).append("]");
                    } else {
                        if (gridSections[y].getStepsFromStart() > 0) {
                            if (gridSections[y].getStepsFromStart() > 9) {
                                result.append("[").append(gridSections[y].getStepsFromStart()).append("]");
                            } else {
                                result.append("[0").append(gridSections[y].getStepsFromStart()).append("]");
                            }
                        } else {
                            result.append("[00]");
                        }
                    }
                }
            }
            result.append("\n");
        }
        return result.toString();
    }

    public static String printMapToLogs(GridSection[][] grid, Map<Point, Room> roomsMap) {
        StringBuilder result = new StringBuilder();
        for (int y = grid.length - 1; y >= 0; y--) {
            for (GridSection[] gridSections : grid) {
                if (roomsMap.containsKey(gridSections[y].getPoint())) {
                    if (nonNull(roomsMap.get(gridSections[y].getPoint()).getRoomContent())) {
                        result.append("[")
                                .append(getLogsIcon(roomsMap.get(gridSections[y].getPoint()).getRoomContent().getRoomType()))
                                .append("]");
                    } else {
                        result.append("[")
                                .append("EM")
                                .append("]");
                    }
                } else {
                    if (gridSections[y].getStepsFromStart() > 0) {
                        if (gridSections[y].getStepsFromStart() > 9) {
                            result.append("[").append(gridSections[y].getStepsFromStart()).append("]");
                        } else {
                            result.append("[0").append(gridSections[y].getStepsFromStart()).append("]");
                        }
                    } else {
                        result.append("[").append("XX").append("]");
                    }
                }
            }
            result.append("\n");
        }
        return result.toString();
    }

    private static String getLogsIcon(RoomType roomType) {
        return switch (roomType) {
            case NORMAL -> "NO";
            case START -> "ST";
            case END -> "EN";
            case TREASURE -> "TR";
            case TREASURE_LOOTED -> "TL";
            case WEREWOLF, VAMPIRE, SWAMP_BEAST, DRAGON, ZOMBIE -> "MO";
            case WEREWOLF_KILLED, VAMPIRE_KILLED, SWAMP_BEAST_KILLED, DRAGON_KILLED, ZOMBIE_KILLED -> "MK";
            case HEALTH_SHRINE -> "HS";
            case MANA_SHRINE -> "MS";
            case ANVIL -> "AN";
            case SHRINE_DRAINED -> "SD";
            case MERCHANT -> "ME";
        };
    }

    private static String getEmptyIcon() {
        return "⬛";
    }

    public static String getDeadEndIcon() {
        return "[XX]";//"\uD83D\uDED1";
    }

    public static String getCrossroadIcon() {
        return "[##]";//"\uD83D\uDD04";
    }

    public static String getCrossroadAndDeadEndWarningIcon() {
        return "\uD83D\uDEAB";
    }
}
