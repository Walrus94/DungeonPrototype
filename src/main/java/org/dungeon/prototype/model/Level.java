package org.dungeon.prototype.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.service.level.WalkerBuilderIterator;
import org.dungeon.prototype.service.room.RandomRoomTypeGenerator;
import org.dungeon.prototype.service.room.RoomTypesCluster;
import org.dungeon.prototype.service.room.WalkerDistributeIterator;

import java.util.*;

import static java.lang.Math.min;
import static org.dungeon.prototype.util.LevelUtil.*;

@Slf4j
public class Level {
    private static final Integer MAX_CROSSROAD_RECURSION_LEVEL = 1;

    public Level(Integer levelNumber) {
        generateLevel(levelNumber);
    }
    @Getter
    private Integer number;
    @Getter
    private Room start;
    private RandomRoomTypeGenerator randomRoomTypeGenerator;
    @Getter
    private GridSection[][] grid;
    private final Queue<WalkerBuilderIterator> waitingWalkers =
            new PriorityQueue<>(Comparator.comparing(WalkerBuilderIterator::getPathFromStart));
    private int deadEnds;
    @Getter
    private LevelMap levelMap;
    @Getter
    private final Map<Integer, Room> deadEndsMap = new TreeMap<>();
    private int maxLength;
    private int minLength;
    private final Map<Point, Room> roomsMap = new HashMap<>();
    private final Random random = new Random();

    private final Queue<WalkerDistributeIterator> pathStarts = new LinkedList<>();

    public Room getRoomByCoordinates(Point currentPoint) {
        return roomsMap.get(currentPoint);
    }

    public void updateRoomType(Point point, Room.Type type) {
        getRoomByCoordinates(point).setType(type);
        grid[point.getX()][point.getY()].setEmoji(getIcon(Optional.of(type)));
    }

    private void generateLevel(Integer levelNumber) {
        log.debug("Generating level {}", levelNumber);
        number = levelNumber;
        val gridSize = calculateGridSize(levelNumber);
        log.debug("Grid size {}", gridSize);
        maxLength = calculateMaxLength(gridSize);
        minLength = calculateMinLength(gridSize);
        log.debug("Corridor length range: {} - {}", minLength, maxLength);

        grid = generateEmptyMapGrid(gridSize);

        val startPoint = Point.of(random.nextInt(gridSize), random.nextInt(gridSize));
        start = buildRoom(startPoint, Room.Type.START);
        roomsMap.put(startPoint, start);
        var currentSection = setStartSection(startPoint);
        log.debug("Successfully built start room, x:{} y:{}", startPoint.getX(), startPoint.getY());
        log.debug("Current map state\n{}", printMap(grid));
        var walkerIterator = WalkerBuilderIterator.builder()
                .currentPoint(currentSection)
                .previousRoom(start)
                .build();
        waitingWalkers.add(walkerIterator);
        log.debug("WalkerIterator initialized: {}", walkerIterator);
        deadEnds = calculateDeadEndsCount(gridSize);
        log.debug("Dead ends count for level: {}", deadEnds);

        while (!waitingWalkers.isEmpty()) {
            processNextStep(waitingWalkers.poll());
        }
        log.debug("Dead Ends: {}", deadEndsMap.entrySet().stream().map(entry -> "{path: " + entry.getKey() + ", room:" + entry.getValue() + "}").toList());
        val endRoom = this.deadEndsMap.entrySet()
                .stream().max(Map.Entry.comparingByKey()).get().getValue();
        log.debug("End room selected: {}", endRoom);
        endRoom.setType(Room.Type.END);

        grid[endRoom.getPoint().getX()][endRoom.getPoint().getY()].setEmoji(getIcon(Optional.of(Room.Type.END)));
        randomRoomTypeGenerator = new RandomRoomTypeGenerator(roomsMap.size() - 2);
        val walker = WalkerDistributeIterator.builder()
                .currentRoom(start)
                .build();
        distributeRoomTypes(walker);
        log.debug("Current map state\n{}", printMap(grid));
        levelMap = new LevelMap(grid[start.getPoint().getX()][start.getPoint().getY()]);
    }

    private WalkerBuilderIterator processNextStep(WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Processing next step...");
        log.debug("Attempting to continue path...");
        var walkerIteratorOptional = getNextSectionAndBuildPath(walkerBuilderIterator);
        if (walkerIteratorOptional.isPresent()) {
            walkerBuilderIterator = walkerIteratorOptional.get();
            log.debug("WalkerIterator successfully retrieved for point {}, direction: {}, previous room point: {}",
                    walkerBuilderIterator.getCurrentPoint(),
                    walkerBuilderIterator.getDirection(),
                    walkerBuilderIterator.getPreviousRoom().getPoint());
            if (deadEnds > 1) {
                log.debug("Processing possible crossroads, dead ends count for level {}", deadEnds);
                val availableDirections = getAvailableDirections(walkerBuilderIterator.getDirection());
                log.debug("Choosing from available directions: {}", availableDirections);
                if (!isPossibleCrossroad(walkerBuilderIterator, minLength, grid.length) &&
                        (random.nextInt(4) % 3 != 0)) {//todo adjust probability
                    log.debug("No crossroads, proceed with single walker");
                    return processNextStep(walkerBuilderIterator);
                } else {
                    log.debug("Splitting roads in {} point", walkerBuilderIterator.getCurrentPoint());
                    log.debug("Current map state\n{}", printMap(grid));
                    walkerBuilderIterator.getCurrentPoint().setCrossroad(true);
                    log.debug("Processing next step for new walker...");
                    var secondWalkerIterator = WalkerBuilderIterator.builder()
                            .currentPoint(walkerBuilderIterator.getCurrentPoint())
                            .direction(walkerBuilderIterator.getDirection())
                            .previousRoom(walkerBuilderIterator.getPreviousRoom())
                            .build();
                    waitingWalkers.add(walkerBuilderIterator);
                    waitingWalkers.add(secondWalkerIterator);
                    deadEnds--;
                    log.debug("Dead ends remaining: {}, active walkers: {}", deadEnds, waitingWalkers);
                    log.debug("Processing next step for walker...");
                    return processNextStep(waitingWalkers.poll());
                }
            } else if (deadEnds == 1) {
                log.debug("Single dead end left, no crossroads");
                return processNextStep(walkerBuilderIterator);
            }
        } else {
            return buildDeadEnd(walkerBuilderIterator);
        }
        return walkerBuilderIterator;
    }

    private WalkerBuilderIterator buildDeadEnd(WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Walker Iterator: {}", walkerBuilderIterator);
        val deadEndSection = walkerBuilderIterator.getCurrentPoint();
        log.debug("Processing dead end for point x:{}, y:{}", deadEndSection.getPoint().getX(), deadEndSection.getPoint().getY());
        if (!deadEndSection.getCrossroad()) {
            val previousRoom = getRoomByCoordinates(getNextSectionInDirection(deadEndSection, getOppositeDirection(walkerBuilderIterator.getDirection())).getPoint());
            log.debug("Previous room: {}", previousRoom);
            log.debug("Building dead end room...");
            val deadEndRoom = buildRoom(deadEndSection.getPoint());
            deadEndRoom.addAdjacentRoom(getOppositeDirection(walkerBuilderIterator.getDirection()), previousRoom);
            deadEndSection.setEmoji(getIcon(Optional.ofNullable(deadEndRoom.getType())));
            deadEndSection.setVisited(true);
            deadEndSection.setStepsFromStart(walkerBuilderIterator.getCurrentPoint().getStepsFromStart() + 1);
            deadEndSection.setDeadEnd(true);
            roomsMap.put(deadEndRoom.getPoint(), deadEndRoom);
            this.deadEndsMap.put(deadEndSection.getStepsFromStart(), deadEndRoom);
            log.debug("Updated dead ends: {}", deadEndsMap.entrySet().stream().map(entry -> "{path: " + entry.getKey() + ", room:" + entry.getValue() + "}").toList());
            log.debug("Current map state\n{}", printMap(grid));
        } else {
            deadEndSection.setDeadEnd(false);
            deadEndSection.setCrossroad(false);
            deadEnds++;
        }
        waitingWalkers.remove(walkerBuilderIterator);
        return walkerBuilderIterator;
    }

    private Optional<WalkerBuilderIterator> getNextSectionAndBuildPath(WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Building next section from x:{}, y:{}, current walker: {}", walkerBuilderIterator.getCurrentPoint().getPoint().getX(), walkerBuilderIterator.getCurrentPoint().getPoint().getY(), walkerBuilderIterator);
        int pathLength;
        val oldDirection = walkerBuilderIterator.getDirection();
        log.debug("Old direction: {}", oldDirection);
        val startSection = walkerBuilderIterator.getCurrentPoint();
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
                    walkerBuilderIterator.setDirection(direction);
                    return Optional.of(buildPathInDirection(grid, walkerBuilderIterator, pathLength));
                }
            }

        return Optional.empty();
    }

    private WalkerBuilderIterator buildPathInDirection(GridSection[][] grid, WalkerBuilderIterator walkerBuilderIterator, int pathLength) {
        GridSection nextStep;
        Room nextRoom;
        Room previousRoom;
        for (int i = 0; i < pathLength; i++) {
            val nextPoint = switch (walkerBuilderIterator.getDirection()) {
                case N:
                    yield Point.of(walkerBuilderIterator.getCurrentPoint().getPoint().getX(),
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY() + 1);
                case E:
                    yield Point.of(walkerBuilderIterator.getCurrentPoint().getPoint().getX() + 1,
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY());
                case S:
                    yield Point.of(walkerBuilderIterator.getCurrentPoint().getPoint().getX(),
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY() - 1);
                case W:
                    yield Point.of(walkerBuilderIterator.getCurrentPoint().getPoint().getX() - 1,
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY());
            };
            log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
            nextRoom = buildRoom(nextPoint);
            previousRoom = walkerBuilderIterator.getPreviousRoom();
            roomsMap.put(nextPoint, nextRoom);
            log.debug("Rooms count: {}", roomsMap.size());
            nextStep = buildNextStep(nextPoint, walkerBuilderIterator, Optional.of(nextRoom.getType()), i);
            previousRoom.addAdjacentRoom(walkerBuilderIterator.getDirection(), nextRoom);
            nextRoom.addAdjacentRoom(getOppositeDirection(walkerBuilderIterator.getDirection()), previousRoom);

            previousRoom = nextRoom;

            walkerBuilderIterator.setCurrentPoint(nextStep);
            walkerBuilderIterator.setPreviousRoom(previousRoom);
        }
        log.debug("Current map state\n{}", printMap(grid));
        return walkerBuilderIterator;
    }

    private GridSection getNextSectionInDirection(GridSection section, Direction direction) {
        return switch (direction) {
            case N -> grid[section.getPoint().getX()][section.getPoint().getY() + 1];
            case E -> grid[section.getPoint().getX() + 1][section.getPoint().getY()];
            case S -> grid[section.getPoint().getX()][section.getPoint().getY() - 1];
            case W -> grid[section.getPoint().getX() - 1][section.getPoint().getY()];
        };
    }

    private GridSection buildNextStep(Point nextPoint, WalkerBuilderIterator walkerBuilderIterator, Optional<Room.Type> roomType, int i) {
        GridSection nextStep = grid[nextPoint.getX()][nextPoint.getY()];
        nextStep.setVisited(true);
        nextStep.setEmoji(getIcon(roomType));
        nextStep.setStepsFromStart(walkerBuilderIterator.getCurrentPoint().getStepsFromStart() + i);
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

    private void distributeRoomTypes(WalkerDistributeIterator walkerIterator) {
        log.debug("Distributing rooms content...");
        while (randomRoomTypeGenerator.hasClusters()) {
            val cluster = randomRoomTypeGenerator.getNextCluster();
            log.debug("Processing cluster - weight: {}, size: {}", cluster.getClusterWeight(), cluster.getRooms().size());
            walkerIterator = distributeRoomTypesCluster(walkerIterator, cluster);
        }
    }

    private WalkerDistributeIterator distributeRoomTypesCluster(WalkerDistributeIterator walkerIterator,
                                            RoomTypesCluster cluster) {
        while (cluster.hasNextRoomToDistribute()) {
            val previousRoom = walkerIterator.getPreviousRoom();
            if (Room.Type.START.equals(walkerIterator.getCurrentRoom().getType())) {
                log.debug("Processing start room...");
                walkerIterator.setCurrentRoom(walkerIterator.getCurrentRoom().getAdjacentRooms().values().stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst().get());
                walkerIterator.setPreviousRoom(start);
            } else {
                val roomsCount = Math.toIntExact(walkerIterator.getCurrentRoom().getAdjacentRooms().values().stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(room -> !room.equals(previousRoom))
                        .count());
                switch (roomsCount) {
                    case 0 -> {
                        if (!walkerIterator.getCurrentRoom().getType().equals(Room.Type.END)) {
                            setRoomContent(walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        }
                        if (!pathStarts.isEmpty()) {
                            walkerIterator = pathStarts.poll();
                        }
                    }
                    case 1 -> {
                        setRoomContent(walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        val nextRoom = walkerIterator.getCurrentRoom().getAdjacentRooms().values().stream()
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(room -> !room.equals(previousRoom))
                                .findFirst().get();
                        walkerIterator.setPreviousRoom(walkerIterator.getCurrentRoom());
                        walkerIterator.setCurrentRoom(nextRoom);
                    }
                    case 2 -> {
                        setRoomContent(walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        val nextRooms = walkerIterator.getCurrentRoom().getAdjacentRooms().values().stream()
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(room -> !room.equals(previousRoom))
                                .toList();
                        pathStarts.offer(WalkerDistributeIterator.builder()
                                .previousRoom(walkerIterator.getCurrentRoom())
                                .currentRoom(nextRooms.get(0))
                                .build());
                        walkerIterator.setPreviousRoom(walkerIterator.getCurrentRoom());
                        walkerIterator.setCurrentRoom(nextRooms.get(1));
                    }
                    case 3 -> {
                        setRoomContent(walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        val nextRooms = walkerIterator.getCurrentRoom().getAdjacentRooms().values().stream()
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(room -> !room.equals(previousRoom))
                                .toList();
                        pathStarts.offer(WalkerDistributeIterator.builder()
                                .previousRoom(walkerIterator.getCurrentRoom())
                                .currentRoom(nextRooms.get(0))
                                .build());
                        pathStarts.offer(WalkerDistributeIterator.builder()
                                .previousRoom(walkerIterator.getCurrentRoom())
                                .currentRoom(nextRooms.get(1))
                                .build());
                        walkerIterator.setPreviousRoom(walkerIterator.getCurrentRoom());
                        walkerIterator.setCurrentRoom(nextRooms.get(2));
                    }
                }
            }
        }
        return walkerIterator;
    }

    private void setRoomContent(Room room, RoomContent roomContent) {
        log.debug("Setting type {} to room [x:{}, y:{}]", roomContent.getRoomType(), room.getPoint().getX(), room.getPoint().getY());
        room.setType(roomContent.getRoomType());
        grid[room.getPoint().getX()][room.getPoint().getY()].setEmoji(getIcon(Optional.of(roomContent.getRoomType())));
        room.setRoomContent(roomContent);
        log.debug("Current map state\n{}", printMap(grid));

    }

    private GridSection setStartSection(Point startPoint) {
        val startSection = grid[startPoint.getX()][startPoint.getY()];
        startSection.setStepsFromStart(0);
        startSection.setEmoji(getIcon(Optional.of(Room.Type.START)));
        startSection.setVisited(true);
        return startSection;
    }
}
