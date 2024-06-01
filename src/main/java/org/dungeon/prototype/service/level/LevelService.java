package org.dungeon.prototype.service.level;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.room.content.EndRoom;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.repository.LevelRepository;
import org.dungeon.prototype.service.room.RandomRoomTypeGenerator;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.service.room.RoomTypesCluster;
import org.dungeon.prototype.service.room.WalkerDistributeIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

import static java.lang.Math.min;
import static org.dungeon.prototype.util.LevelUtil.calculateDeadEndsCount;
import static org.dungeon.prototype.util.LevelUtil.calculateGridSize;
import static org.dungeon.prototype.util.LevelUtil.calculateMaxLength;
import static org.dungeon.prototype.util.LevelUtil.calculateMinLength;
import static org.dungeon.prototype.util.LevelUtil.generateEmptyMapGrid;
import static org.dungeon.prototype.util.LevelUtil.getAvailableDirections;
import static org.dungeon.prototype.util.LevelUtil.getIcon;
import static org.dungeon.prototype.util.LevelUtil.getNextPointInDirection;
import static org.dungeon.prototype.util.LevelUtil.getOppositeDirection;
import static org.dungeon.prototype.util.LevelUtil.isPossibleCrossroad;
import static org.dungeon.prototype.util.LevelUtil.printMap;

@Slf4j
@Component
public class LevelService {
    private final Random random = new Random();
    private final Map<Long,Queue<WalkerBuilderIterator>> waitingWalkers = new HashMap<>();
    private final Map<Long, RandomRoomTypeGenerator> randomRoomTypeGenerators = new HashMap<>();
    private final Map<Long, Queue<WalkerDistributeIterator>> pathStarts = new HashMap<>();
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private RoomService roomService;

    public Level saveNewLevel(Long chatId,  Integer levelNumber) {
        var level = generateLevel(chatId, levelNumber);
        if (levelRepository.existsByChatId(chatId)) {
            levelRepository.removeByChatId(chatId);
        }
        return levelRepository.save(level);
    }

    public Level updateLevel(Level level) {
        return levelRepository.save(level);
    }

    public Level getLevel(Long chatId) {
        return levelRepository.findByChatId(chatId).orElseGet(() -> {
            log.error("Unable to find level by chatId: {}", chatId);
            return null;
        });
    }

    public Integer getLevelNumber(Long chatId) {
        //TODO: adjust query
        Level level = levelRepository.findByChatId(chatId).orElseGet(() -> {
            log.error("Unable to fetch level number by chatId");
            return null;
        });
        return level.getNumber();
    }

    public void remove(Long chatId) {
        levelRepository.removeByChatId(chatId);
    }

    public boolean hasLevel(Long chatId) {
        return levelRepository.existsByChatId(chatId);
    }

    public Level generateLevel(Long chatId, Integer levelNumber) {
        log.debug("Generating level {}", levelNumber);
        val gridSize = calculateGridSize(levelNumber);
        var level = new Level();
        level.setChatId(chatId);
        level.setNumber(levelNumber);
        log.debug("Grid size {}", gridSize);
        val maxLength = calculateMaxLength(gridSize);
        val minLength = calculateMinLength(gridSize);
        log.debug("Corridor length range: {} - {}", minLength, maxLength);
        level.setMaxLength(maxLength);
        level.setMinLength(minLength);

        level.setGrid(generateEmptyMapGrid(gridSize));

        val startPoint = new Point(random.nextInt(gridSize), random.nextInt(gridSize));
        var start = buildStartRoom(startPoint, chatId);
        level.setStart(start);
        Map<Point, Room> roomsMap = new HashMap<>();
        roomsMap.put(startPoint, start);
        level.setRoomsMap(roomsMap);
        var currentSection = setStartSection(level.getGrid(), startPoint);
        log.debug("Successfully built start room, x:{} y:{}", startPoint.getX(), startPoint.getY());
        log.debug("Current map state\n{}", printMap(level.getGrid()));
        var walkerIterator = WalkerBuilderIterator.builder()
                .currentPoint(currentSection)
                .previousRoom(start)
                .build();
        if (waitingWalkers.containsKey(chatId)) {
            waitingWalkers.get(chatId).offer(walkerIterator);
        } else {
            var queue = new PriorityQueue<>(Comparator.comparing(WalkerBuilderIterator::getPathFromStart));
            queue.offer(walkerIterator);
            waitingWalkers.put(chatId, queue);
        }
        log.debug("WalkerIterator initialized: {}", walkerIterator);
        val deadEnds = calculateDeadEndsCount(gridSize);
        log.debug("Dead ends count for level: {}", deadEnds);
        level.setDeadEnds(deadEnds);
        Map<Integer, Room> deadEndsMap = new TreeMap<>();
        level.setDeadEndsMap(deadEndsMap);
        while (!waitingWalkers.get(chatId).isEmpty()) {
            processNextStep(level, waitingWalkers.get(chatId).poll());
        }
        log.debug("Dead Ends: {}", deadEndsMap.entrySet().stream().map(entry -> "{path: " + entry.getKey() + ", room:" + entry.getValue() + "}").toList());
        val endRoom = level.getDeadEndsMap().entrySet()
                .stream().max(Map.Entry.comparingByKey()).get().getValue();
        log.debug("End room selected: {}", endRoom);
        endRoom.setRoomContent(new EndRoom());

        level.getGrid()[endRoom.getPoint().getX()][endRoom.getPoint().getY()].setEmoji(getIcon(Optional.of(RoomType.END)));
        randomRoomTypeGenerators.put(chatId, new RandomRoomTypeGenerator(roomsMap.size() - 2));
        val walker = WalkerDistributeIterator.builder()
                .currentRoom(start)
                .build();
        distributeRoomTypes(level, walker);
        log.debug("Current map state\n{}", printMap(level.getGrid()));
        val levelMap = new LevelMap(level.getGrid()[start.getPoint().getX()][start.getPoint().getY()]);
        level.setLevelMap(levelMap);
        return level;
    }

    private WalkerBuilderIterator processNextStep(Level level, WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Processing next step...");
        log.debug("Attempting to continue path...");
        var walkerIteratorOptional = getNextSectionAndBuildPath(level, walkerBuilderIterator);
        if (walkerIteratorOptional.isPresent()) {
            walkerBuilderIterator = walkerIteratorOptional.get();
            log.debug("WalkerIterator successfully retrieved for point {}, direction: {}, previous room point: {}",
                    walkerBuilderIterator.getCurrentPoint(),
                    walkerBuilderIterator.getDirection(),
                    walkerBuilderIterator.getPreviousRoom().getPoint());
            if (level.getDeadEnds() > 1) {
                log.debug("Processing possible crossroads, dead ends count for level {}", level.getDeadEnds());
                val availableDirections = getAvailableDirections(walkerBuilderIterator.getDirection());
                log.debug("Choosing from available directions: {}", availableDirections);
                if (!isPossibleCrossroad(walkerBuilderIterator, level.getMinLength(), level.getGrid().length) &&
                        (random.nextInt(4) % 3 != 0)) {//TODO: adjust probability according to level depth
                    log.debug("No crossroads, proceed with single walker");
                    return processNextStep(level, walkerBuilderIterator);
                } else {
                    log.debug("Splitting roads in {} point", walkerBuilderIterator.getCurrentPoint());
                    log.debug("Current map state\n{}", printMap(level.getGrid()));
                    walkerBuilderIterator.getCurrentPoint().setCrossroad(true);
                    log.debug("Processing next step for new walker...");
                    var secondWalkerIterator = WalkerBuilderIterator.builder()
                            .currentPoint(walkerBuilderIterator.getCurrentPoint())
                            .direction(walkerBuilderIterator.getDirection())
                            .previousRoom(walkerBuilderIterator.getPreviousRoom())
                            .build();
                    waitingWalkers.get(level.getChatId()).offer(walkerBuilderIterator);
                    waitingWalkers.get(level.getChatId()).offer(secondWalkerIterator);
                    level.decreaseDeadEnds();
                    log.debug("Dead ends remaining: {}, active walkers: {}", level.getDeadEnds(), waitingWalkers);
                    log.debug("Processing next step for walker...");
                    return processNextStep(level, waitingWalkers.get(level.getChatId()).poll());
                }
            } else if (level.getDeadEnds() == 1) {
                log.debug("Single dead end left, no crossroads");
                return processNextStep(level, walkerBuilderIterator);
            }
        } else {
            return buildDeadEnd(level, walkerBuilderIterator);
        }
        return walkerBuilderIterator;
    }

    private WalkerBuilderIterator buildDeadEnd(Level level, WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Walker Iterator: {}", walkerBuilderIterator);
        val deadEndSection = walkerBuilderIterator.getCurrentPoint();
        log.debug("Processing dead end for point x:{}, y:{}", deadEndSection.getPoint().getX(), deadEndSection.getPoint().getY());
        if (!deadEndSection.getCrossroad()) {
            val previousRoom = level.getRoomByCoordinates(getNextSectionInDirection(level.getGrid(), deadEndSection, getOppositeDirection(walkerBuilderIterator.getDirection())).getPoint());
            log.debug("Previous room: {}", previousRoom);
            log.debug("Building dead end room...");
            val deadEndRoom = buildRoom(deadEndSection.getPoint(), level.getChatId());
            deadEndRoom.addAdjacentRoom(getOppositeDirection(walkerBuilderIterator.getDirection()));
            roomService.saveOrUpdateRoom(deadEndRoom);
            deadEndSection.setEmoji(getIcon(Optional.ofNullable(deadEndRoom.getRoomContent().getRoomType())));
            deadEndSection.setVisited(true);
            deadEndSection.setStepsFromStart(walkerBuilderIterator.getCurrentPoint().getStepsFromStart() + 1);
            deadEndSection.setDeadEnd(true);
            level.getRoomsMap().put(deadEndRoom.getPoint(), deadEndRoom);
            level.getDeadEndsMap().put(deadEndSection.getStepsFromStart(), deadEndRoom);
            log.debug("Updated dead ends: {}", level.getDeadEndsMap().entrySet().stream().map(entry -> "{path: " + entry.getKey() + ", room:" + entry.getValue() + "}").toList());
            log.debug("Current map state\n{}", printMap(level.getGrid()));
        } else {
            deadEndSection.setDeadEnd(false);
            deadEndSection.setCrossroad(false);
            level.increaseDeadEnds();
        }
        waitingWalkers.get(level.getChatId()).remove(walkerBuilderIterator);
        return walkerBuilderIterator;
    }

    private Optional<WalkerBuilderIterator> getNextSectionAndBuildPath(Level level, WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Building next section from x:{}, y:{}, current walker: {}", walkerBuilderIterator.getCurrentPoint().getPoint().getX(), walkerBuilderIterator.getCurrentPoint().getPoint().getY(), walkerBuilderIterator);
        int pathLength;
        val oldDirection = walkerBuilderIterator.getDirection();
        log.debug("Old direction: {}", oldDirection);
        val startSection = walkerBuilderIterator.getCurrentPoint();
        val availableDirections = getAvailableDirections(oldDirection);
        log.debug("Available directions: {}", availableDirections);
        for (Direction direction : availableDirections) {
            log.debug("Calculating length in direction: {}", direction);
            val maxLengthInDirection = calculateMaxLengthInDirection(level.getGrid(), startSection, direction);
            log.debug("Max length in {} direction is {}", direction, maxLengthInDirection);
            if (maxLengthInDirection < level.getMinLength()) {
                continue;
            }
            val max = min(level.getMaxLength(), maxLengthInDirection);
            pathLength = random.nextInt(max - level.getMinLength() + 1) + level.getMinLength();
            if (pathLength >= level.getMinLength() && pathLength <= level.getMaxLength()) {
                log.debug("Random path length to walk: {}", pathLength);
                walkerBuilderIterator.setDirection(direction);
                return Optional.of(buildPathInDirection(level, walkerBuilderIterator, pathLength));
            }
        }

        return Optional.empty();
    }

    private WalkerBuilderIterator buildPathInDirection(Level level, WalkerBuilderIterator walkerBuilderIterator, int pathLength) {
        GridSection nextStep;
        Room nextRoom;
        Room previousRoom;
        for (int i = 0; i < pathLength; i++) {
            val nextPoint = switch (walkerBuilderIterator.getDirection()) {
                case N:
                    yield new Point(walkerBuilderIterator.getCurrentPoint().getPoint().getX(),
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY() + 1);
                case E:
                    yield new Point(walkerBuilderIterator.getCurrentPoint().getPoint().getX() + 1,
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY());
                case S:
                    yield new Point(walkerBuilderIterator.getCurrentPoint().getPoint().getX(),
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY() - 1);
                case W:
                    yield new Point(walkerBuilderIterator.getCurrentPoint().getPoint().getX() - 1,
                            walkerBuilderIterator.getCurrentPoint().getPoint().getY());
            };
            log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
            nextRoom = buildRoom(nextPoint, level.getChatId());
            nextRoom = roomService.saveOrUpdateRoom(nextRoom);
            previousRoom = walkerBuilderIterator.getPreviousRoom();
            level.getRoomsMap().put(nextPoint, nextRoom);
            log.debug("Rooms count: {}", level.getRoomsMap().size());
            nextStep = buildNextStep(level.getGrid(), nextPoint, walkerBuilderIterator, Optional.of(nextRoom.getRoomContent().getRoomType()), i);
            previousRoom.addAdjacentRoom(walkerBuilderIterator.getDirection());
            previousRoom = roomService.saveOrUpdateRoom(previousRoom);
            nextRoom.addAdjacentRoom(getOppositeDirection(walkerBuilderIterator.getDirection()));
            roomService.saveOrUpdateRoom(previousRoom);
            previousRoom = nextRoom;

            walkerBuilderIterator.setCurrentPoint(nextStep);
            walkerBuilderIterator.setPreviousRoom(previousRoom);
        }
        log.debug("Current map state\n{}", printMap(level.getGrid()));
        return walkerBuilderIterator;
    }

    private GridSection getNextSectionInDirection(GridSection[][] grid, GridSection section, Direction direction) {
        return switch (direction) {
            case N -> grid[section.getPoint().getX()][section.getPoint().getY() + 1];
            case E -> grid[section.getPoint().getX() + 1][section.getPoint().getY()];
            case S -> grid[section.getPoint().getX()][section.getPoint().getY() - 1];
            case W -> grid[section.getPoint().getX() - 1][section.getPoint().getY()];
        };
    }

    private GridSection buildNextStep(GridSection[][] grid, Point nextPoint, WalkerBuilderIterator walkerBuilderIterator, Optional<RoomType> roomType, int i) {
        GridSection nextStep = grid[nextPoint.getX()][nextPoint.getY()];
        nextStep.setVisited(true);
        nextStep.setEmoji(getIcon(roomType));
        nextStep.setStepsFromStart(walkerBuilderIterator.getCurrentPoint().getStepsFromStart() + i);
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

    private void distributeRoomTypes(Level level, WalkerDistributeIterator walkerIterator) {
        log.debug("Distributing rooms content...");
        pathStarts.put(level.getChatId(), new LinkedList<>());
        while (randomRoomTypeGenerators.get(level.getChatId()).hasClusters()) {
            val cluster = randomRoomTypeGenerators.get(level.getChatId()).getNextCluster();
            log.debug("Processing cluster - weight: {}, size: {}", cluster.getClusterWeight(), cluster.getRooms().size());
            walkerIterator = distributeRoomTypesCluster(level, walkerIterator, cluster);
        }
    }

    private WalkerDistributeIterator distributeRoomTypesCluster(Level level, WalkerDistributeIterator walkerIterator,
                                                                RoomTypesCluster cluster) {
        while (cluster.hasNextRoomToDistribute()) {
            val previousRoom = walkerIterator.getPreviousRoom();
            if (RoomType.START.equals(walkerIterator.getCurrentRoom().getRoomContent().getRoomType())) {
                log.debug("Processing start room...");
                var currentRoom = walkerIterator.getCurrentRoom();
                walkerIterator.setCurrentRoom(currentRoom.getAdjacentRooms().entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                        .map(level::getRoomByCoordinates)
                        .findFirst().get());
                walkerIterator.setPreviousRoom(level.getStart());
            } else {
                val currentRoom = walkerIterator.getCurrentRoom();
                val roomsCount = Math.toIntExact(currentRoom.getAdjacentRooms().entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                        .filter(point -> !point.equals(previousRoom.getPoint()))
                        .count());
                switch (roomsCount) {
                    case 0 -> {
                        if (!RoomType.END.equals(walkerIterator.getCurrentRoom().getRoomContent().getRoomType())) {
                            setRoomContent(level.getGrid(), walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        }
                        if (!level.getPathStarts().isEmpty()) {
                            walkerIterator = level.getPathStarts().poll();
                        }
                    }
                    case 1 -> {
                        setRoomContent(level.getGrid(), walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        val nextRoom = walkerIterator.getCurrentRoom().getAdjacentRooms().entrySet().stream()
                                .filter(Map.Entry::getValue)
                                .map(Map.Entry::getKey)
                                .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                                .filter(point -> !point.equals(previousRoom.getPoint()))
                                .map(level::getRoomByCoordinates)
                                .findFirst().get();
                        walkerIterator.setPreviousRoom(walkerIterator.getCurrentRoom());
                        walkerIterator.setCurrentRoom(nextRoom);
                    }
                    case 2 -> {
                        setRoomContent(level.getGrid(), walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        val nextRooms = walkerIterator.getCurrentRoom().getAdjacentRooms().entrySet().stream()
                                .filter(Map.Entry::getValue)
                                .map(Map.Entry::getKey)
                                .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                                .filter(point -> !point.equals(previousRoom.getPoint()))
                                .map(level::getRoomByCoordinates)
                                .toList();
                        pathStarts.get(level.getChatId()).offer(WalkerDistributeIterator.builder()
                                .previousRoom(walkerIterator.getCurrentRoom())
                                .currentRoom(nextRooms.get(0))
                                .build());
                        walkerIterator.setPreviousRoom(walkerIterator.getCurrentRoom());
                        walkerIterator.setCurrentRoom(nextRooms.get(1));
                    }
                    case 3 -> {
                        setRoomContent(level.getGrid(), walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                        val nextRooms = walkerIterator.getCurrentRoom().getAdjacentRooms().entrySet().stream()
                                .filter(Map.Entry::getValue)
                                .map(Map.Entry::getKey)
                                .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                                .filter(point -> !point.equals(previousRoom.getPoint()))
                                .map(level::getRoomByCoordinates)
                                .toList();
                        pathStarts.get(level.getChatId()).offer(WalkerDistributeIterator.builder()
                                .previousRoom(walkerIterator.getCurrentRoom())
                                .currentRoom(nextRooms.get(0))
                                .build());
                        pathStarts.get(level.getChatId()).offer(WalkerDistributeIterator.builder()
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

    private void setRoomContent(GridSection[][] grid, Room room, RoomContent roomContent) {
        log.debug("Setting type {} to room [x:{}, y:{}]", roomContent.getRoomType(), room.getPoint().getX(), room.getPoint().getY());
        room.setRoomContent(roomContent);
        grid[room.getPoint().getX()][room.getPoint().getY()].setEmoji(getIcon(Optional.of(roomContent.getRoomType())));
        room.setRoomContent(roomContent);
        log.debug("Current map state\n{}", printMap(grid));

    }

    public Room buildRoom(Point point, Long chatId) {
        return new Room(point, chatId);
    }

    private Room buildStartRoom(Point point, Long chatId) {
        return new Room(point, chatId, new EmptyRoom(RoomType.START));
    }

    private GridSection setStartSection(GridSection[][] grid, Point startPoint) {
        val startSection = grid[startPoint.getX()][startPoint.getY()];
        startSection.setStepsFromStart(0);
        startSection.setEmoji(getIcon(Optional.of(RoomType.START)));
        startSection.setVisited(true);
        return startSection;
    }
}
