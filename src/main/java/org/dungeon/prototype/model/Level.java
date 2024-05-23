package org.dungeon.prototype.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.WalkerIterator;
import org.dungeon.prototype.service.WeightedRandomRoomTypeGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static java.lang.Math.min;
import static org.dungeon.prototype.util.LevelUtil.Direction;
import static org.dungeon.prototype.util.LevelUtil.buildRoom;
import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfMonsters;
import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfRooms;
import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfTreasures;
import static org.dungeon.prototype.util.LevelUtil.calculateDeadEndsCount;
import static org.dungeon.prototype.util.LevelUtil.calculateGridSize;
import static org.dungeon.prototype.util.LevelUtil.calculateMaxLength;
import static org.dungeon.prototype.util.LevelUtil.calculateMinLength;
import static org.dungeon.prototype.util.LevelUtil.generateEmptyMapGrid;
import static org.dungeon.prototype.util.LevelUtil.getAvailableDirections;
import static org.dungeon.prototype.util.LevelUtil.getIcon;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.printMap;

@Slf4j
public class Level {

    public Level(Integer levelNumber) {
        generateLevel(levelNumber);
    }
    @Getter
    private Room start;
    private WeightedRandomRoomTypeGenerator roomTypeWeightsGenerator = new WeightedRandomRoomTypeGenerator();
    @Getter
    private GridSection[][] grid;
    @Getter
    private final Set<Room> deadEnds = new HashSet<>();
    private final Map<Point, Room> roomsMap = new HashMap<>();
    private final Random random = new Random();

    public Room getRoomByCoordinates(Point currentPoint) {
        return roomsMap.get(currentPoint);
    }

    private Level generateLevel(Integer levelNumber) {
        log.debug("Generating level {}", levelNumber);
        val gridSize = calculateGridSize(levelNumber);
        log.debug("Grid size {}", gridSize);
        val roomTotal = calculateAmountOfRooms(gridSize);
        log.debug("Room total {}", roomTotal);
        val maxLength = calculateMaxLength(gridSize);
        val minLength = calculateMinLength(gridSize);
        log.debug("Corridor length range: {} - {}",minLength, maxLength);

        grid = generateEmptyMapGrid(gridSize);

        val roomMonsters = calculateAmountOfMonsters(roomTotal);
        val roomTreasures = calculateAmountOfTreasures(roomTotal);
        roomTypeWeightsGenerator = new WeightedRandomRoomTypeGenerator();//TODO refactor to configure depending on level

        val startPoint = Point.of(random.nextInt(gridSize), random.nextInt(gridSize));
        start = buildRoom(startPoint, Room.Type.START);
        roomsMap.put(startPoint, start);
        var currentSection = setStartSection(startPoint);
        log.debug("Successfully built start room, x:{} y:{}", startPoint.getX(), startPoint.getY());
        log.debug("Current map state\n{}", printMap(grid));
        var walkerIterator = WalkerIterator.of(currentSection, null, start, roomMonsters, roomTreasures);
        log.debug("WalkerIterator initialized: {}", walkerIterator);
        var deadEnds = calculateDeadEndsCount(roomTotal);
        log.debug("Dead ends count for level: {}", deadEnds);
        var roomsCount = roomTotal;

        while (roomsCount > 0) {
            var processedRooms = processNextStep(walkerIterator, roomsCount, maxLength, minLength, deadEnds);
            roomsCount = roomsCount - processedRooms;
        }
        val endRoom = this.deadEnds
                .stream().min((a, b) -> getGridSection(a.getPoint()).getStepsFromStart()
                        .compareTo(getGridSection(b.getPoint()).getStepsFromStart())).get();
        endRoom.setType(Room.Type.END);
        grid[endRoom.getPoint().getX()][endRoom.getPoint().getY()].setEmoji(getIcon(Optional.of(Room.Type.END)));
        log.debug("Current map state\n{}", printMap(grid));
        return this;
    }

    private GridSection getGridSection(Point point) {
        return grid[point.getX()][point.getY()];
    }

    private int processNextStep(WalkerIterator walkerIterator, int roomsCount, int maxLength, int minLength, int deadEnds) {
        log.debug("Processing next step...");
        if (roomsCount > 0) {
            log.debug("Attempting to continue path...");
            var walkerIteratorOptional = getNextSectionAndBuildPath(new WalkerIterator(walkerIterator), maxLength, minLength);
            if (walkerIteratorOptional.isPresent()) {
                walkerIterator = walkerIteratorOptional.get();
                log.debug("WalkerIterator successfully retrieved for point {}, direction: {}, previous room point: {}",
                        walkerIterator.getCurrentPoint(), walkerIterator.getDirection(), walkerIterator.getPreviousRoom().getPoint());
                if (deadEnds > 1) {
                    log.debug("Processing possible crossroads, dead ends count for level {}", deadEnds);
                    val availableDirections = getAvailableDirections(walkerIterator.getDirection());
                    log.debug("Choosing from available directions: {}", availableDirections);
                    if (random.nextInt(2) == 0) { // random between 1 and 0
                        log.debug("No crossroads, proceed with single walker");
                        return processNextStep(walkerIterator, roomsCount, maxLength, minLength, deadEnds);
                    } else {
                        log.debug("Splitting roads...");
                        deadEnds--;
                        log.debug("Processing next step for new walker...");
                        val secondWalkerMonsterRooms = walkerIterator.getRoomMonsters() / 2;
                        val secondWalkerTreasureRooms = walkerIterator.getRoomTreasures() / 2;
                        val secondWalkerIterator = new WalkerIterator(walkerIterator);
                        secondWalkerIterator.setRoomMonsters(secondWalkerMonsterRooms);
                        secondWalkerIterator.setRoomTreasures(secondWalkerTreasureRooms);
                        val roomsTwo = processNextStep(secondWalkerIterator, roomsCount, maxLength, minLength, deadEnds);
                        log.debug("Rooms count from new walker: {}", roomsTwo);
                        log.debug("Processing next step for old walker...");
                        walkerIterator.setRoomMonsters(walkerIterator.getRoomMonsters() - secondWalkerMonsterRooms);
                        walkerIterator.setRoomTreasures(walkerIterator.getRoomTreasures() - secondWalkerTreasureRooms);
                        val roomsOne = processNextStep(walkerIterator, roomsCount, maxLength, minLength, deadEnds);
                        log.debug("Rooms count from old walker: {}", roomsOne);
                        return roomsOne + roomsTwo;
                    }
                } else if (deadEnds == 1) {
                    log.debug("Single dead end left, no crossroads");
                    return processNextStep(walkerIterator, roomsCount, maxLength, minLength, deadEnds);
                }
            } else {
                return buildDeadEnd(walkerIterator, roomsCount);
            }
        } else {
            log.debug("No rooms left!");
            return roomsCount;
        }
        return roomsCount;
    }

    private int buildDeadEnd(WalkerIterator walkerIterator, int roomsCount) {
        val deadEndSection = walkerIterator.getCurrentPoint();
        log.debug("Building dead end, x:{}, y:{}", deadEndSection.getCoordinates().getX(), deadEndSection.getCoordinates().getY());
        log.debug("Walker Iterator: {}", walkerIterator);
//        val previousRoom = walkerIterator.getPreviousRoom(); todo: fix walkerIterator to store previous room after building path
        val previousRoom = getRoomByCoordinates(getNextSectionInDirection(deadEndSection, getOppositeDirection(walkerIterator.getDirection())).getCoordinates());
        log.debug("Previous room: {}", previousRoom);
        val deadEndRoom = buildRoom(deadEndSection.getCoordinates(), generateRoomType(walkerIterator));
        deadEndRoom.addAdjacentRoom(getOppositeDirection(walkerIterator.getDirection()), previousRoom);
        deadEndSection.setEmoji(getIcon(Optional.ofNullable(deadEndRoom.getType())));
        deadEndSection.setVisited(true);
        deadEndSection.setStepsFromStart(walkerIterator.getCurrentPoint().getStepsFromStart() + 1);
        deadEndSection.setDeadEnd(true);
        roomsMap.put(deadEndRoom.getPoint(), deadEndRoom);
        this.deadEnds.add(deadEndRoom);
        log.debug("Updated dead ends: {}", deadEnds.stream().map(Room::getPoint).toList());
        log.debug("Current map state\n{}", printMap(grid));
        return roomsCount;
    }

    private Optional<WalkerIterator> getNextSectionAndBuildPath(WalkerIterator walkerIterator, int maxLength, int minLength) {
        log.debug("Building next section from x:{}, y:{}, current walker: {}", start.getPoint().getX(), start.getPoint().getY(), walkerIterator);
        int pathLength;
        val oldDirection = walkerIterator.getDirection();
        log.debug("Old direction: {}", oldDirection);
        val startSection = walkerIterator.getCurrentPoint();
        val availableDirections = getAvailableDirections(oldDirection);
        log.debug("Available directions: {}", availableDirections);
            for (Direction direction : availableDirections) {
                log.debug("Calculating length in direction: {}", direction);
                val maxLengthInDirection = calculateMaxLengthInDirection(grid, startSection, direction);
                log.debug("Max length in {} direction is {}", direction, maxLengthInDirection);
                if (maxLengthInDirection < minLength) {
                    continue;
                }
                val max = min(maxLength, maxLengthInDirection);
                pathLength = random.nextInt(max - minLength + 1) + minLength;
                if (pathLength >= minLength && pathLength <= maxLength) {
                    log.debug("Random path length to walk: {}", pathLength);
                    walkerIterator.setDirection(direction);
                    return buildPathInDirection(grid, walkerIterator, pathLength);
                }
            }
        return Optional.empty();
    }

    private Optional<WalkerIterator> buildPathInDirection(GridSection[][] grid, WalkerIterator walkerIterator, int pathLength) {
        GridSection nextStep;
        Room nextRoom;
        Room previousRoom;
        for (int i = 0; i < pathLength; i++) {
            val nextPoint = switch (walkerIterator.getDirection()) {
                case N:
                    yield Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX(),
                            walkerIterator.getCurrentPoint().getCoordinates().getY() + 1);
                case E:
                    yield Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX() + 1,
                            walkerIterator.getCurrentPoint().getCoordinates().getY());
                case S:
                    yield Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX(),
                            walkerIterator.getCurrentPoint().getCoordinates().getY() - 1);
                case W:
                    yield Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX() - 1,
                            walkerIterator.getCurrentPoint().getCoordinates().getY());
            };
            log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
            nextRoom = buildRoom(nextPoint, generateRoomType(walkerIterator));
            previousRoom = walkerIterator.getPreviousRoom();
            roomsMap.put(nextPoint, nextRoom);
            nextStep = buildNextStep(nextPoint, walkerIterator, Optional.of(nextRoom.getType()), i);
            previousRoom.addAdjacentRoom(walkerIterator.getDirection(), nextRoom);
            nextRoom.addAdjacentRoom(getOppositeDirection(walkerIterator.getDirection()), previousRoom);

            previousRoom = nextRoom;

            walkerIterator.setCurrentPoint(nextStep);
            walkerIterator.setPreviousRoom(previousRoom);
        }
        log.debug("Current map state\n{}", printMap(grid));
        return Optional.of(walkerIterator);
    }

    private Room.Type generateRoomType(WalkerIterator walkerIterator) {
        log.debug("Generating room type...");
        log.debug("Monster room left: {}, Treasures room left: {}", walkerIterator.getRoomTreasures(), walkerIterator.getRoomMonsters());
        List<Room.Type> exclude = new ArrayList<>();
        if (walkerIterator.getRoomMonsters() == 0) {
            exclude.add(Room.Type.MONSTER);
        }
        if (walkerIterator.getRoomTreasures() == 0) {
            exclude.add(Room.Type.TREASURE);
        }
        val roomType = roomTypeWeightsGenerator.nextRoomType(exclude);
        log.debug("Room type: {}", roomType);
        if (roomType == Room.Type.MONSTER) {
            walkerIterator.setRoomMonsters(walkerIterator.getRoomMonsters() - 1);
        }
        if (roomType == Room.Type.TREASURE) {
            walkerIterator.setRoomTreasures(walkerIterator.getRoomTreasures() - 1);
        }
        return roomType;
    }

    private GridSection getNextSectionInDirection(GridSection section, Direction direction) {
        return switch (direction) {
            case N -> grid[section.getCoordinates().getX()][section.getCoordinates().getY() + 1];
            case E -> grid[section.getCoordinates().getX() + 1][section.getCoordinates().getY()];
            case S -> grid[section.getCoordinates().getX()][section.getCoordinates().getY() - 1];
            case W -> grid[section.getCoordinates().getX() - 1][section.getCoordinates().getY()];
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

    private Integer calculateMaxLengthInDirection(GridSection[][] grid, GridSection startSection, Direction direction) {
        log.debug("Calculating max length in {} direction...", direction);
        GridSection nextSection = null;
        var path = 0;
        while (nextSection == null || !nextSection.getVisited()) {
            if (nextSection == null) {
                nextSection = startSection;
            }
            switch (direction) {
                case N -> {
                    if (nextSection.getCoordinates().getY() + 1 > grid.length - 1 ||
                            grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() + 1].getVisited() || (
                            nextSection.getCoordinates().getY() + 2 <= grid.length - 1 &&
                            grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() + 2].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() + 1];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
                }
                case E -> {
                    if (nextSection.getCoordinates().getX() + 1 > grid.length - 1 ||
                            grid[nextSection.getCoordinates().getX() + 1][nextSection.getCoordinates().getY()].getVisited() || (
                            nextSection.getCoordinates().getX() + 2 <= grid.length - 1 &&
                            grid[nextSection.getCoordinates().getX() + 2][nextSection.getCoordinates().getY()].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getCoordinates().getX() + 1][nextSection.getCoordinates().getY()];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
                }
                case S -> {
                    if (nextSection.getCoordinates().getY() - 1 < 0 ||
                            grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() - 1].getVisited() || (
                            nextSection.getCoordinates().getY() - 2 >= 0 &&
                            grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() - 2].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() - 1];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
                }
                case W -> {
                    if (nextSection.getCoordinates().getX() - 1 < 0 ||
                            grid[startSection.getCoordinates().getX() - 1][startSection.getCoordinates().getY()].getVisited() || (
                            nextSection.getCoordinates().getX() - 2 >= 0 &&
                            grid[startSection.getCoordinates().getX() - 2][startSection.getCoordinates().getY()].getVisited())) {
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getCoordinates().getX() - 1][nextSection.getCoordinates().getY()];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
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
