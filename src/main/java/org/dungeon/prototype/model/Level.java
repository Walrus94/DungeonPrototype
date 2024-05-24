package org.dungeon.prototype.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.model.ui.level.WalkerIterator;
import org.dungeon.prototype.service.WeightedRandomRoomTypeGenerator;

import java.util.*;

import static java.lang.Math.min;
import static org.dungeon.prototype.util.LevelUtil.Direction;
import static org.dungeon.prototype.util.LevelUtil.buildRoom;
import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfRooms;
import static org.dungeon.prototype.util.LevelUtil.calculateDeadEndsCount;
import static org.dungeon.prototype.util.LevelUtil.calculateGridSize;
import static org.dungeon.prototype.util.LevelUtil.calculateMaxLength;
import static org.dungeon.prototype.util.LevelUtil.calculateMinLength;
import static org.dungeon.prototype.util.LevelUtil.generateEmptyMapGrid;
import static org.dungeon.prototype.util.LevelUtil.getAvailableDirections;
import static org.dungeon.prototype.util.LevelUtil.getIcon;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.isPossibleCrossroad;
import static org.dungeon.prototype.util.LevelUtil.printMap;

@Slf4j
public class Level {

    public Level(Integer levelNumber) {
        generateLevel(levelNumber);
    }
    @Getter
    private Room start;
    private WeightedRandomRoomTypeGenerator roomTypeWeightsGenerator;
    @Getter
    private GridSection[][] grid;
    private final Queue<WalkerIterator> waitingWalkers =
            new PriorityQueue<>(Comparator.comparing(WalkerIterator::getPathFromStart));
    private int deadEnds;
    @Getter
    private LevelMap levelMap;
    @Getter
    private final Map<Integer, Room> deadEndsMap = new TreeMap<>();
    private int roomTotal;
    private int maxLength;
    private int minLength;
    private final Map<Point, Room> roomsMap = new HashMap<>();
    private final Random random = new Random();

    public Room getRoomByCoordinates(Point currentPoint) {
        return roomsMap.get(currentPoint);
    }

    public void updateRoomType(Point point, Room.Type type) {
        getRoomByCoordinates(point).setType(type);
        grid[point.getX()][point.getY()].setEmoji(getIcon(Optional.of(type)));
    }

    private void generateLevel(Integer levelNumber) {
        log.debug("Generating level {}", levelNumber);
        val gridSize = calculateGridSize(levelNumber);
        log.debug("Grid size {}", gridSize);
        roomTotal = calculateAmountOfRooms(gridSize);
        log.debug("Room total {}", roomTotal);
        maxLength = calculateMaxLength(gridSize);
        minLength = calculateMinLength(gridSize);
        log.debug("Corridor length range: {} - {}", minLength, maxLength);

        grid = generateEmptyMapGrid(gridSize);

        roomTypeWeightsGenerator = new WeightedRandomRoomTypeGenerator(roomTotal);

        val startPoint = Point.of(random.nextInt(gridSize), random.nextInt(gridSize));
        start = buildRoom(startPoint, Room.Type.START);
        start.setVisitedByPlayer(true);
        roomsMap.put(startPoint, start);
        var currentSection = setStartSection(startPoint);
        log.debug("Successfully built start room, x:{} y:{}", startPoint.getX(), startPoint.getY());
        log.debug("Current map state\n{}", printMap(grid));
        var walkerIterator = WalkerIterator.builder()
                .currentPoint(currentSection)
                .previousRoom(start)
                .build();
        waitingWalkers.add(walkerIterator);
        log.debug("WalkerIterator initialized: {}", walkerIterator);
        deadEnds = calculateDeadEndsCount(roomTotal);
        log.debug("Dead ends count for level: {}", deadEnds);

        while (!waitingWalkers.isEmpty()) {
            processNextStep(waitingWalkers.poll());
        }
        val endRoom = this.deadEndsMap.entrySet()
                .stream().max(Map.Entry.comparingByKey()).get().getValue();
        endRoom.setType(Room.Type.END);
        grid[endRoom.getPoint().getX()][endRoom.getPoint().getY()].setEmoji(getIcon(Optional.of(Room.Type.END)));
        log.debug("Current map state\n{}", printMap(grid));
        levelMap = new LevelMap(grid[start.getPoint().getX()][start.getPoint().getY()]);
    }

    private WalkerIterator processNextStep(WalkerIterator walkerIterator) {
        log.debug("Processing next step...");
        if (roomsMap.size() < roomTotal) {
            log.debug("Attempting to continue path...");
            var walkerIteratorOptional = getNextSectionAndBuildPath(walkerIterator);
            if (walkerIteratorOptional.isPresent()) {
                walkerIterator = walkerIteratorOptional.get();
                log.debug("WalkerIterator successfully retrieved for point {}, direction: {}, previous room point: {}",
                        walkerIterator.getCurrentPoint(), walkerIterator.getDirection(), walkerIterator.getPreviousRoom().getPoint());
                if (deadEnds > 1) {
                    log.debug("Processing possible crossroads, dead ends count for level {}", deadEnds);
                    val availableDirections = getAvailableDirections(walkerIterator.getDirection());
                    log.debug("Choosing from available directions: {}", availableDirections);
                    if (!isPossibleCrossroad(walkerIterator, minLength, grid.length) &&
                            random.nextInt(2) == 0) {//todo adjust probability
                        log.debug("No crossroads, proceed with single walker");
                        return processNextStep(walkerIterator);
                    } else {
                        log.debug("Splitting roads in {} point", walkerIterator.getCurrentPoint());
                        log.debug("Current map state\n{}", printMap(grid));
                        walkerIterator.getCurrentPoint().setCrossroad(true);
                        log.debug("Processing next step for new walker...");
                        var secondWalkerIterator = WalkerIterator.builder()
                                .currentPoint(walkerIterator.getCurrentPoint())
                                .direction(walkerIterator.getDirection())
                                .previousRoom(walkerIterator.getPreviousRoom())
                                .build();
                        waitingWalkers.add(walkerIterator);
                        waitingWalkers.add(secondWalkerIterator);
                        deadEnds--;
                        log.debug("Dead ends remaining: {}, active walkers: {}", deadEnds, waitingWalkers);
                        log.debug("Processing next step for walker...");
                        return processNextStep(waitingWalkers.poll());
                    }
                } else if (deadEnds == 1) {
                    log.debug("Single dead end left, no crossroads");
                    return processNextStep(walkerIterator);
                }
            } else {
                return buildDeadEnd(walkerIterator);
            }
        } else {
            log.debug("No rooms left!");
            return walkerIterator;
        }
        return walkerIterator;
    }

    private WalkerIterator buildDeadEnd(WalkerIterator walkerIterator) {
        log.debug("Walker Iterator: {}", walkerIterator);
        val deadEndSection = walkerIterator.getCurrentPoint();
        log.debug("Processing dead end for point x:{}, y:{}", deadEndSection.getPoint().getX(), deadEndSection.getPoint().getY());
        if (!deadEndSection.getCrossroad()) {
            val previousRoom = getRoomByCoordinates(getNextSectionInDirection(deadEndSection, getOppositeDirection(walkerIterator.getDirection())).getPoint());
            log.debug("Previous room: {}", previousRoom);
            log.debug("Building dead end room...");
            val deadEndRoom = buildRoom(deadEndSection.getPoint(), generateRoomType());
            deadEndRoom.addAdjacentRoom(getOppositeDirection(walkerIterator.getDirection()), previousRoom);
            deadEndSection.setEmoji(getIcon(Optional.ofNullable(deadEndRoom.getType())));
            deadEndSection.setVisited(true);
            deadEndSection.setStepsFromStart(walkerIterator.getCurrentPoint().getStepsFromStart() + 1);
            deadEndSection.setDeadEnd(true);
            roomsMap.put(deadEndRoom.getPoint(), deadEndRoom);
            this.deadEndsMap.put(deadEndSection.getStepsFromStart(), deadEndRoom);
            log.debug("Updated dead ends: {}", deadEndsMap.entrySet().stream().map(entry -> "{path: " + entry.getKey() + ", room:" + entry.getValue() + "}").toList());
            log.debug("Current map state\n{}", printMap(grid));
        }
        waitingWalkers.remove(walkerIterator);
        return walkerIterator;
    }

    private Optional<WalkerIterator> getNextSectionAndBuildPath(WalkerIterator walkerIterator) {
        log.debug("Building next section from x:{}, y:{}, current walker: {}", walkerIterator.getCurrentPoint().getPoint().getX(), walkerIterator.getCurrentPoint().getPoint().getY(), walkerIterator);
        int pathLength;
        val oldDirection = walkerIterator.getDirection();
        log.debug("Old direction: {}", oldDirection);
        val startSection = walkerIterator.getCurrentPoint();
        val availableDirections = getAvailableDirections(oldDirection);
        log.debug("Available directions: {}", availableDirections);
            for (Direction direction : availableDirections) {
                log.debug("Calculating length in direction: {}", direction);
                val maxLengthInDirection = calculateMaxLengthInDirection(startSection, direction);
                log.debug("Max length in {} direction is {}", direction, maxLengthInDirection);
                if (maxLengthInDirection < minLength) {
                    continue;
                }
                val max = min(maxLength, maxLengthInDirection);
                pathLength = random.nextInt(max - minLength + 1) + minLength;
                if (pathLength >= minLength && pathLength <= maxLength) {
                    log.debug("Random path length to walk: {}", pathLength);
                    walkerIterator.setDirection(direction);
                    return Optional.of(buildPathInDirection(grid, walkerIterator, pathLength));
                }
            }

        return Optional.empty();
    }

    private WalkerIterator buildPathInDirection(GridSection[][] grid, WalkerIterator walkerIterator, int pathLength) {
        GridSection nextStep;
        Room nextRoom;
        Room previousRoom;
        for (int i = 0; i < pathLength; i++) {
            val nextPoint = switch (walkerIterator.getDirection()) {
                case N:
                    yield Point.of(walkerIterator.getCurrentPoint().getPoint().getX(),
                            walkerIterator.getCurrentPoint().getPoint().getY() + 1);
                case E:
                    yield Point.of(walkerIterator.getCurrentPoint().getPoint().getX() + 1,
                            walkerIterator.getCurrentPoint().getPoint().getY());
                case S:
                    yield Point.of(walkerIterator.getCurrentPoint().getPoint().getX(),
                            walkerIterator.getCurrentPoint().getPoint().getY() - 1);
                case W:
                    yield Point.of(walkerIterator.getCurrentPoint().getPoint().getX() - 1,
                            walkerIterator.getCurrentPoint().getPoint().getY());
            };
            log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
            nextRoom = buildRoom(nextPoint, generateRoomType());
            previousRoom = walkerIterator.getPreviousRoom();
            roomsMap.put(nextPoint, nextRoom);
            log.debug("Rooms count: {} out of {} total rooms", roomsMap.size(), roomTotal);
            nextStep = buildNextStep(nextPoint, walkerIterator, Optional.of(nextRoom.getType()), i);
            previousRoom.addAdjacentRoom(walkerIterator.getDirection(), nextRoom);
            nextRoom.addAdjacentRoom(getOppositeDirection(walkerIterator.getDirection()), previousRoom);

            previousRoom = nextRoom;

            walkerIterator.setCurrentPoint(nextStep);
            walkerIterator.setPreviousRoom(previousRoom);
        }
        log.debug("Current map state\n{}", printMap(grid));
        return walkerIterator;
    }

    private Room.Type generateRoomType() {
        log.debug("Generating room type...");
        log.debug("Monster room left: {}, Treasures room left: {}", roomTypeWeightsGenerator.getRoomTreasures(), roomTypeWeightsGenerator.getRoomMonsters());
        val roomType = roomTypeWeightsGenerator.nextRoomType();
        log.debug("Room type: {}", roomType);
        return roomType;
    }

    private GridSection getNextSectionInDirection(GridSection section, Direction direction) {
        return switch (direction) {
            case N -> grid[section.getPoint().getX()][section.getPoint().getY() + 1];
            case E -> grid[section.getPoint().getX() + 1][section.getPoint().getY()];
            case S -> grid[section.getPoint().getX()][section.getPoint().getY() - 1];
            case W -> grid[section.getPoint().getX() - 1][section.getPoint().getY()];
        };
    }

    private GridSection buildNextStep(Point nextPoint, WalkerIterator walkerIterator, Optional<Room.Type> roomType, int i) {
        GridSection nextStep = grid[nextPoint.getX()][nextPoint.getY()];
        nextStep.setVisited(true);
        nextStep.setEmoji(getIcon(roomType));
        nextStep.setStepsFromStart(walkerIterator.getCurrentPoint().getStepsFromStart() + i);
        grid[nextPoint.getX()][nextPoint.getY()] = nextStep;
        return nextStep;
    }

    private Integer calculateMaxLengthInDirection(GridSection startSection, Direction direction) {
        log.debug("Calculating max length in {} direction...", direction);
        GridSection nextSection = null;
        var path = 0;
        while (nextSection == null || !nextSection.getVisited()) {
            if (nextSection == null) {
                nextSection = startSection;
            }
            switch (direction) {
                case N -> {
                    if (nextSection.getPoint().getY() + 1 > grid.length - 1 ||
                            grid[nextSection.getPoint().getX()][nextSection.getPoint().getY() + 1].getVisited() || (
                            nextSection.getPoint().getY() + 2 <= grid.length - 1 &&
                            grid[nextSection.getPoint().getX()][nextSection.getPoint().getY() + 2].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getPoint().getX()][nextSection.getPoint().getY() + 1];
                    log.debug("Adding {} step...", nextSection.getPoint());
                }
                case E -> {
                    if (nextSection.getPoint().getX() + 1 > grid.length - 1 ||
                            grid[nextSection.getPoint().getX() + 1][nextSection.getPoint().getY()].getVisited() || (
                            nextSection.getPoint().getX() + 2 <= grid.length - 1 &&
                            grid[nextSection.getPoint().getX() + 2][nextSection.getPoint().getY()].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getPoint().getX() + 1][nextSection.getPoint().getY()];
                    log.debug("Adding {} step...", nextSection.getPoint());
                }
                case S -> {
                    if (nextSection.getPoint().getY() - 1 < 0 ||
                            grid[nextSection.getPoint().getX()][nextSection.getPoint().getY() - 1].getVisited() || (
                            nextSection.getPoint().getY() - 2 >= 0 &&
                            grid[nextSection.getPoint().getX()][nextSection.getPoint().getY() - 2].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getPoint().getX()][nextSection.getPoint().getY() - 1];
                    log.debug("Adding {} step...", nextSection.getPoint());
                }
                case W -> {
                    if (nextSection.getPoint().getX() - 1 < 0 ||
                            grid[startSection.getPoint().getX() - 1][startSection.getPoint().getY()].getVisited() || (
                            nextSection.getPoint().getX() - 2 >= 0 &&
                            grid[startSection.getPoint().getX() - 2][startSection.getPoint().getY()].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getPoint().getX() - 1][nextSection.getPoint().getY()];
                    log.debug("Adding {} step...", nextSection.getPoint());
                }
            }
            path++;
            log.debug("Current path length: {}", path);
        }
        return path;
    }

    private GridSection setStartSection(Point startPoint) {
        val startSection = grid[startPoint.getX()][startPoint.getY()];
        startSection.setStepsFromStart(0);
        startSection.setEmoji(getIcon(Optional.of(Room.Type.START)));
        startSection.setVisited(true);
        return startSection;
    }
}
