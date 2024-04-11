package org.dungeon.prototype.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.WalkerIterator;
import org.dungeon.prototype.service.WeightedRandomRoomTypeGenerator;

import java.util.*;

import static java.lang.Math.*;
import static org.dungeon.prototype.util.LevelUtil.*;

@Slf4j
@NoArgsConstructor
public class Level {
    public enum Direction {
        N, E, S, W

    }
    public Level(Room start, GridSection[][] grid) {
        this.start = start;
        this.grid = grid;
    }
    @Getter
    private Room start;
    private WeightedRandomRoomTypeGenerator roomTypeWeightsGenerator = new WeightedRandomRoomTypeGenerator();
    private int roomMonsters;
    private int roomTreasures;
    @Getter
    private GridSection[][] grid;
    @Getter
    private final Set<Room> deadEnds = new HashSet<>();

    private final Random random = new Random();

    public Level generateLevel(Integer levelNumber) {
        log.debug("Generating level {}", levelNumber);
        val gridSize = calculateGridSize(levelNumber);
        log.debug("Grid size {}", gridSize);
        val roomTotal = calculateAmountOfRooms(gridSize);
        log.debug("Room total {}", roomTotal);
        val maxLength = calculateMaxLength(gridSize);
        log.debug("Max corridor length for level: {}", maxLength);

        grid = generateEmptyMapGrid(gridSize);
        log.debug("Generated empty grid:\n{}", printMap(grid));

        roomMonsters = calculateAmountOfMonsters(roomTotal);
        roomTreasures = calculateAmountOfTreasures(roomTotal);
        roomTypeWeightsGenerator = new WeightedRandomRoomTypeGenerator();//TODO refactor to configure depending on level

        val startPoint = Point.of(random.nextInt(gridSize), random.nextInt(gridSize));
        start = buildRoom(null, startPoint, Room.Type.START);
        log.debug("Successfully built start room, x:{} y:{}", startPoint.getX(), startPoint.getY());
        var currentSection = setStartSection(startPoint, Optional.of(Room.Type.START));
        var walkerIterator = WalkerIterator.of(currentSection, null, null);
        var deadEnds = calculateDeadEndsCount(roomTotal);
        log.debug("Dead ends count for level: {}", deadEnds);
        var roomsCount = roomTotal;

        while (roomsCount > 0) {
            var processedRooms = processNextStep(walkerIterator, start, roomsCount, maxLength, deadEnds);
            roomsCount = roomsCount - processedRooms;
        }
        val endRoom = this.deadEnds
                .stream().min((a, b) -> getGridSection(a.getPoint()).getStepsFromStart()
                        .compareTo(getGridSection(b.getPoint()).getStepsFromStart())).get();
        endRoom.setType(Room.Type.END);
        grid[endRoom.getPoint().getX()][endRoom.getPoint().getY()].setEmoji(getIcon(Optional.of(Room.Type.END)));
        return this;
    }

    private GridSection getGridSection(Point point) {
        return grid[point.getX()][point.getY()];
    }

    private int processNextStep(WalkerIterator walkerIterator, Room currentRoom, int roomsCount, int maxLength , int deadEnds) {
        GridSection currentSection;
        log.debug("Processing next step...");
        if (roomsCount > 0) {
            log.debug("Attempting to continue path...");
            var walkerIteratorOptional = getNextSectionAndBuildPath(walkerIterator, currentRoom, maxLength);
            if (walkerIteratorOptional.isPresent()) {
                walkerIterator = walkerIteratorOptional.get();
                log.debug("WalkerIterator successfully retrieved for point {}, old direction: {}",
                        walkerIterator.getCurrentPoint(), walkerIterator.getPreviousDirection());
                currentRoom = buildRoom(walkerIterator.getPreviousRoom(),
                        walkerIterator.getCurrentPoint().getCoordinates(), genereteRoomType());
                currentSection = grid[walkerIterator.getCurrentPoint().getCoordinates().getX()][walkerIterator.getCurrentPoint().getCoordinates().getY()];
                currentSection.setEmoji(getIcon(Optional.ofNullable(currentRoom.getType())));
                if (deadEnds > 1) {
                    log.debug("Processing possible crossroads, dead ends count for level {}", deadEnds);
                    val availableDirections = getAvailableDirections(walkerIterator.getPreviousDirection());
                    log.debug("Choosing from available directions: {}", availableDirections);
                    if (random.nextInt(2) == 0) { // random between 1 and 0
                        log.debug("No crossroads, proceed with single walker");
                        return processNextStep(walkerIterator, currentRoom, roomsCount, maxLength, deadEnds);
                    } else {
                        log.debug("Splitting roads...");
                        deadEnds--;
                        log.debug("Processing next step for new walker...");
                        val roomsTwo = processNextStep(new WalkerIterator(walkerIterator), currentRoom, roomsCount, maxLength, deadEnds);
                        log.debug("Rooms count from new walker: {}", roomsTwo);
                        log.debug("Processing next step for old walker...");
                        val roomsOne = processNextStep(walkerIterator, currentRoom, roomsCount, maxLength, deadEnds);
                        log.debug("Rooms count from old walker: {}", roomsOne);
                        return roomsOne + roomsTwo;
                    }
                } else if (deadEnds == 1) {
                    log.debug("Single dead end left, no crossroads");
                    walkerIteratorOptional = getNextSectionAndBuildPath(walkerIterator, currentRoom, maxLength);
                    if (walkerIteratorOptional.isPresent()) {
                        walkerIterator = walkerIteratorOptional.get();
                        return processNextStep(walkerIterator, currentRoom, roomsCount, maxLength, deadEnds);
                    } else {
                        return buildDeadEnd(walkerIterator, currentRoom, roomsCount);
                    }
                }
            } else {
                return buildDeadEnd(walkerIterator, currentRoom, roomsCount);
            }
        } else {
            log.debug("No rooms left!");
            return roomsCount;
        }
        return roomsCount;
    }

    private int buildDeadEnd(WalkerIterator walkerIterator, Room currentRoom, int roomsCount) {
        log.debug("Building dead end, x:{}, y:{}", currentRoom.getPoint().getX(), currentRoom.getPoint().getY());
        val deadEnd = currentRoom.getPoint();
        val deadEndSection = grid[deadEnd.getX()][deadEnd.getY()];
        deadEndSection.setVisited(true);
        deadEndSection.setStepsFromStart(walkerIterator.getCurrentPoint().getStepsFromStart() + 1);
        deadEndSection.setEmoji(getIcon(Optional.ofNullable(currentRoom.getType())));
        deadEndSection.setDeadEnd(true);
        this.deadEnds.add(currentRoom);
        return roomsCount;
    }

    private List<Level.Direction> getAvailableDirections(Level.Direction oldDirection) {
        if (oldDirection == null) {
            return List.of(Level.Direction.values());
        }
        return Arrays.stream(Level.Direction.values()).filter(dir ->
                !dir.equals(oldDirection) && ! dir.equals(getOppsiteDirection(oldDirection))).toList();
    }

    private Optional<WalkerIterator> getNextSectionAndBuildPath(WalkerIterator walkerIterator, Room start, int maxLength) {
        log.debug("Building next section from x:{}, y:{}, current walker: {}", start.getPoint().getX(), start.getPoint().getY(), walkerIterator);
        int pathLength = 0;
        int iteration = 0;
        Level.Direction direction = null;
        val oldDirection = walkerIterator.getPreviousDirection();
        log.debug("Old direction: {}", oldDirection);
        val startSection = walkerIterator.getCurrentPoint();
        val availableDirections = getAvailableDirections(oldDirection);
        log.debug("Available directions: {}", availableDirections);
        while (pathLength < 1) {
            if (iteration > Level.Direction.values().length) {
                log.debug("Failed to build path in any direction!");
                return Optional.empty();
            }
            direction = availableDirections.get(random.nextInt(availableDirections.size()));
            log.debug("Chosen direction: {}", direction);
            val maxLengthInDirection = calculateMaxLengthInDirection(grid, startSection, direction);
            log.debug("Max length in {} direction is {}", direction, maxLengthInDirection);
            pathLength = maxLengthInDirection == 0 ? maxLengthInDirection :
                    random.nextInt(min(maxLength + 1, maxLengthInDirection + 1) - 1) + 1 ;
            log.debug("Random path length to walk: {}", pathLength);
            iteration++;
        }
        if (direction == null) {
            return Optional.empty();
        }
        walkerIterator.setPreviousDirection(direction);
        return buildPathInDirection(grid, walkerIterator, start, pathLength);
    }

    private Optional<WalkerIterator> buildPathInDirection(GridSection[][] grid, WalkerIterator walkerIterator, Room start, int pathLength) {
        GridSection nextStep = null;
        Room nextRoom;
        Room previousRoom = start;
        switch (walkerIterator.getPreviousDirection()) {
            case N -> {
                for (int i = 0; i < pathLength; i++) {
                    val nextPoint = Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX(),
                            walkerIterator.getCurrentPoint().getCoordinates().getY() + 1);
                    log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
                    nextRoom = buildRoom(previousRoom, nextPoint, genereteRoomType());
                    nextStep = buildNextStep(nextPoint, walkerIterator, Optional.ofNullable(nextRoom.getType()), i);
                    previousRoom.setMiddle(nextRoom);
                    previousRoom = nextRoom;
                    walkerIterator.setCurrentPoint(nextStep);
                    walkerIterator.setPreviousRoom(previousRoom);
                }
            }
            case E -> {
                for (int i = 0; i < pathLength; i++) {
                    val nextPoint = Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX() + 1,
                            walkerIterator.getCurrentPoint().getCoordinates().getY());
                    log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
                    nextRoom = buildRoom(previousRoom, nextPoint, genereteRoomType());
                    nextStep = buildNextStep(nextPoint, walkerIterator, Optional.of(nextRoom.getType()), i);
                    previousRoom.setMiddle(nextRoom);
                    previousRoom = nextRoom;

                    walkerIterator.setCurrentPoint(nextStep);
                    walkerIterator.setPreviousRoom(previousRoom);
                }
            }
            case S -> {
                for (int i = 0; i < pathLength; i++) {
                    val nextPoint = Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX(),
                            walkerIterator.getCurrentPoint().getCoordinates().getY() - 1);
                    log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
                    nextRoom = buildRoom(previousRoom, nextPoint, genereteRoomType());
                    nextStep = buildNextStep(nextPoint, walkerIterator, Optional.of(nextRoom.getType()), i);
                    previousRoom.setMiddle(nextRoom);
                    previousRoom = nextRoom;

                    walkerIterator.setCurrentPoint(nextStep);
                    walkerIterator.setPreviousRoom(previousRoom);
                }
            }
            case W -> {
                for (int i = 0; i < pathLength; i++) {
                    val nextPoint = Point.of(walkerIterator.getCurrentPoint().getCoordinates().getX() - 1,
                            walkerIterator.getCurrentPoint().getCoordinates().getY());
                    log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
                    nextRoom = buildRoom(previousRoom, nextPoint, genereteRoomType());
                    nextStep = buildNextStep(nextPoint, walkerIterator, Optional.ofNullable(nextRoom.getType()), i);
                    previousRoom.setMiddle(nextRoom);
                    previousRoom = nextRoom;

                    walkerIterator.setCurrentPoint(nextStep);
                    walkerIterator.setPreviousRoom(previousRoom);
                }
            }
        }
        log.debug("Current map state\n{}", printMap(grid));
        return nextStep == null ? Optional.empty() : Optional.of(WalkerIterator.of(nextStep, walkerIterator.getPreviousDirection(), previousRoom));
    }

    private Room.Type genereteRoomType() {
        log.debug("Generating room type...");
        log.debug("Monster room left: {}, Treasures room left: {}", roomMonsters, roomMonsters);
        List<Room.Type> exclude = new ArrayList<>();
        if (roomMonsters == 0) {
            exclude.add(Room.Type.MONSTER);
        }
        if (roomTreasures == 0) {
            exclude.add(Room.Type.TREASURE);
        }
        val roomType = roomTypeWeightsGenerator.nextRoomType(exclude);
        log.debug("Room type: {}", roomType);
        if (roomType == Room.Type.MONSTER) {
            roomMonsters--;
        }
        if (roomType == Room.Type.TREASURE) {
            roomTreasures--;
        }
        return roomType;
    }

    private GridSection buildNextStep(Point nextPoint, WalkerIterator walkerIterator, Optional<Room.Type> roomType, int i) {
        GridSection nextStep = grid[nextPoint.getX()][nextPoint.getY()];
        nextStep.setVisited(true);
        nextStep.setEmoji(getIcon(roomType));
        nextStep.setStepsFromStart(walkerIterator.getCurrentPoint().getStepsFromStart() + i);
        grid[nextPoint.getX()][nextPoint.getY()] = nextStep;
        return nextStep;
    }

    private Integer calculateMaxLengthInDirection(GridSection[][] grid, GridSection startSection, Level.Direction direction) {
        log.debug("Calculating max length in {} direction...", direction);
        GridSection nextSection = null;
        var path = 0;
        while (nextSection == null || !nextSection.getVisited()) {
            if (nextSection == null) {
                nextSection = startSection;
            }
            switch (direction) {
                case N -> {
                    if (nextSection.getCoordinates().getY() + 1 >= grid.length ||
                            grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() + 1].getVisited()) { //TODO refactor for rectangle map
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() + 1];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
                }
                case E -> {
                    if (nextSection.getCoordinates().getX() + 1 >= grid.length ||
                            grid[nextSection.getCoordinates().getX() + 1][nextSection.getCoordinates().getY()].getVisited()) { //TODO refactor for rectangle map
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getCoordinates().getX() + 1][nextSection.getCoordinates().getY()];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
                }
                case S -> {
                    if (nextSection.getCoordinates().getY() - 1 < 0 ||
                            grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() - 1].getVisited()) { //TODO refactor for rectangle map
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[nextSection.getCoordinates().getX()][nextSection.getCoordinates().getY() - 1];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
                }
                case W -> {
                    if (nextSection.getCoordinates().getX() - 1 < 0 ||
                            grid[startSection.getCoordinates().getX() - 1][startSection.getCoordinates().getY()].getVisited()) { //TODO refactor for rectangle map
                        log.debug("Edge of map reached, returning max length of {}", path);
                        return path;
                    }
                    nextSection = grid[startSection.getCoordinates().getX() - 1][startSection.getCoordinates().getY()];
                    log.debug("Adding {} step...", nextSection.getCoordinates());
                }
            }
            path++;
            log.debug("Current path length: {}", path);
            //TODO investigate why path draws to inf
            if (path > grid.length) {
                return grid.length;
            }
        }
        return path;
    }

    public static Direction getOppsiteDirection(Direction direction) {
        return switch (direction) {
            case N -> Direction.S;
            case E -> Direction.W;
            case S -> Direction.N;
            case W -> Direction.E;
        };
    }

    private GridSection setStartSection(Point startPoint, Optional<Room.Type> roomType) {
        val startSection = grid[startPoint.getX()][startPoint.getY()];
        startSection.setStepsFromStart(0);
        startSection.setEmoji(getIcon(roomType));
        startSection.setVisited(true);
        return startSection;
    }
}
