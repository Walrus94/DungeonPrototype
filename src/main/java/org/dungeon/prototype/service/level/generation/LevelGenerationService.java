package org.dungeon.prototype.service.level.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.room.content.EndRoom;
import org.dungeon.prototype.model.room.content.NormalRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.content.StartRoom;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.LevelMap;
import org.dungeon.prototype.properties.GenerationProperties;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.service.room.generation.RandomRoomTypeGenerator;
import org.dungeon.prototype.service.room.generation.RoomTypesCluster;
import org.dungeon.prototype.service.room.generation.WalkerDistributeIterator;
import org.dungeon.prototype.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.min;
import static org.apache.commons.math3.util.FastMath.toIntExact;
import static org.dungeon.prototype.util.LevelUtil.*;
import static org.dungeon.prototype.util.LevelUtil.getNextPointInDirection;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;

@Slf4j
@Service
public class LevelGenerationService {
    private final Map<Long,Queue<WalkerBuilderIterator>> waitingBuildWalkers = new HashMap<>();
    @Autowired
    private RoomService roomService;
    @Autowired
    private RandomRoomTypeGenerator randomRoomTypeGenerator;
    @Autowired
    private GenerationProperties generationProperties;

    /**
     * Generates level and fills with content
     * @param chatId id of player's chat
     * @param player player starting level
     * @param levelNumber number of level
     * @return generated level
     */
    public Level generateLevel(Long chatId, Player player, Integer levelNumber) {
        log.debug("Generating level {}", levelNumber);
        //initializing level and calculating basic attributes
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

        //empty level grid will be filled with rooms during generation
        level.setGrid(generateEmptyMapGrid(gridSize));

        //randomly picking start of the level
        val startPoint = new Point(getRandomInt(minLength, gridSize - minLength - 1),
                getRandomInt(minLength, gridSize - minLength - 1));
        var start = buildStartRoom(startPoint, chatId);
        start = roomService.saveOrUpdateRoom(start);
        level.setStart(start);
        //initializing level's room map
        Map<Point, Room> roomsMap = new HashMap<>();
        roomsMap.put(startPoint, start);
        level.setRoomsMap(roomsMap);
        //Initializing first walker builder iterator, that will start paving the way from start room
        var currentSection = setStartSection(level.getGrid(), startPoint);
        log.debug("Successfully built start room, x:{} y:{}", startPoint.getX(), startPoint.getY());
        log.debug("Current map state\n{}", printMap(level.getGrid()));
        var walkerIterator = WalkerBuilderIterator.builder()
                .currentPoint(currentSection)
                .previousRoom(start)
                .build();
        if (waitingBuildWalkers.containsKey(chatId)) {
            waitingBuildWalkers.get(chatId).offer(walkerIterator);
        } else {
            //Initializing walkers queue ordered by path from start
            var queue = new PriorityQueue<>(Comparator.comparing(WalkerBuilderIterator::getPathFromStart));
            queue.offer(walkerIterator);
            waitingBuildWalkers.put(chatId, queue);
        }
        log.debug("WalkerIterator initialized: {}", walkerIterator);
        while (!waitingBuildWalkers.get(chatId).isEmpty()) {
            processNextStep(level, waitingBuildWalkers.get(chatId).poll());
        }
        log.debug("Dead Ends: {}", level.getDeadEnds().stream().map(deadEnd -> "{path: " + deadEnd.getStepsFromStart() + ", room:" + deadEnd.getPoint() + "}").toList());
        if (Objects.isNull(level.getEnd())) {
            log.debug("Missed end room during level map generation, selecting from dead ends");
            val endSection = level.getDeadEnds()
                    .stream().max(Comparator.comparing(GridSection::getStepsFromStart)).get();
            endSection.setDeadEnd(false);
            level.removeDeadEnd(endSection);
            endSection.setEmoji(getIcon(Optional.of(RoomType.END)));
            val endRoom = level.getRoomByCoordinates(endSection.getPoint());
            level.setEnd(endRoom);
            log.debug("End room selected: {}", endRoom);
            endRoom.setRoomContent(new EndRoom());
        }
        log.debug("Current map state\n{}", printMap(level.getGrid()));
        log.debug("Map properties: total rooms - {}, deadEnds - {}. segments - {}",
                level.getRoomsMap().size(),
                level.getDeadEnds().size(),
                level.getDeadEndToSegmentMap().size());
        log.debug("Dead ends: {}", level.getDeadEnds().stream().map(GridSection::getPoint).toList());
        log.debug("Segments: {}", new ArrayList<>(level.getDeadEndToSegmentMap().values()));
        //now fill generated level rooms with content (monsters, treasures etc.)
        distributeRoomTypes(level, player);
        log.debug("Current map state\n{}", printMap(level.getGrid()));
        val levelMap = new LevelMap(level.getGrid()[start.getPoint().getX()][start.getPoint().getY()]);
        level.setLevelMap(levelMap);
        return level;
    }

    /**
     * Attempts to continue path of current walker
     * @param level being generated
     * @param walkerBuilderIterator current walker
     * @return updated walker after processing step
     */
    private WalkerBuilderIterator processNextStep(Level level, WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Processing next step...");
        log.debug("Attempting to continue path...");
        var walkerIteratorOptional = getNextSectionAndBuildPath(level, walkerBuilderIterator);
        if (walkerIteratorOptional.isPresent()) {//Optional is empty if walker is unable to continue path from current point
            walkerBuilderIterator = walkerIteratorOptional.get();
            log.debug("WalkerIterator successfully retrieved for point {}, direction: {}, previous room point: {}",
                    walkerBuilderIterator.getCurrentPoint(),
                    walkerBuilderIterator.getDirection(),
                    walkerBuilderIterator.getPreviousRoom().getPoint());
            log.debug("Processing possible crossroads...");
            if (isCrossroad(walkerBuilderIterator, level, waitingBuildWalkers.get(level.getChatId()).size())) {
                log.debug("Splitting roads in {} point", walkerBuilderIterator.getCurrentPoint());
                log.debug("Current map state\n{}", printMap(level.getGrid()));
                log.debug("Generating new walker...");
                walkerBuilderIterator.getCurrentPoint().setCrossroad(true);
                //initializing new walker in the same point
                var secondWalkerIterator = WalkerBuilderIterator.builder()
                        .currentPoint(walkerBuilderIterator.getCurrentPoint())
                        .direction(walkerBuilderIterator.getDirection())
                        .roomsSegment(new RoomsSegment(walkerBuilderIterator.getCurrentPoint()))
                        .previousRoom(walkerBuilderIterator.getPreviousRoom())
                        .build();
                //puts both in corresponding queue
                waitingBuildWalkers.get(level.getChatId()).offer(walkerBuilderIterator);
                waitingBuildWalkers.get(level.getChatId()).offer(secondWalkerIterator);
                log.debug("Active walkers: {}", waitingBuildWalkers);
                log.debug("Processing next step for walker...");
                //recursive call for next step with walker from queue
                return processNextStep(level, waitingBuildWalkers.get(level.getChatId()).poll());
            } else {
                log.debug("No crossroads, proceed with single walker");
                //recursive call for same walker
                return processNextStep(level, walkerBuilderIterator);
            }
        } else {
            //building dead end if unable to continue path
            return buildDeadEnd(level, walkerBuilderIterator);
        }
    }

    /**
     * Builds dead end in active walker current point
     * @param level being generated
     * @param walkerBuilderIterator active walker
     * @return updated walker
     */
    private WalkerBuilderIterator buildDeadEnd(Level level, WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Walker Iterator: {}", walkerBuilderIterator);
        val deadEndSection = walkerBuilderIterator.getCurrentPoint();
        log.debug("Processing dead end for point x:{}, y:{}", deadEndSection.getPoint().getX(), deadEndSection.getPoint().getY());
        if (!deadEndSection.getCrossroad()) {
            log.debug("Building dead end room...");
            if (walkerBuilderIterator.hasRoomsSegment() && walkerBuilderIterator.hasOpenRoomsSegment()) {
                var deadEndRoom = buildRoom(deadEndSection.getPoint(), level.getChatId());
                deadEndRoom.addAdjacentRoom(getOppositeDirection(walkerBuilderIterator.getDirection()));
                deadEndRoom = roomService.saveOrUpdateRoom(deadEndRoom);
                deadEndSection.setEmoji(getIcon(Optional.ofNullable(isNull(deadEndRoom.getRoomContent()) ?
                        null : deadEndRoom.getRoomContent().getRoomType())));
                deadEndSection.setVisited(true);
                level.getRoomsMap().put(deadEndRoom.getPoint(), deadEndRoom);
                deadEndSection.setDeadEnd(true);
                level.getDeadEnds().add(deadEndSection);
                walkerBuilderIterator.getRoomsSegment().setEnd(deadEndSection);
                level.getDeadEndToSegmentMap().put(deadEndRoom.getPoint(), walkerBuilderIterator.getRoomsSegment());
                log.debug("Updated dead ends: {}", level.getDeadEnds().stream().map(deadEnd -> "{path: " + deadEnd.getStepsFromStart() + ", room:" + deadEnd.getPoint() + "}").toList());
                log.debug("Current map state\n{}", printMap(level.getGrid()));
            } else {
                var endRoom = buildEndRoom(deadEndSection.getPoint(), level.getChatId());
                endRoom.addAdjacentRoom(getOppositeDirection(walkerBuilderIterator.getDirection()));
                endRoom = roomService.saveOrUpdateRoom(endRoom);
                deadEndSection.setEmoji(getIcon(Optional.of(RoomType.END)));
                deadEndSection.setVisited(true);
                log.debug("End room selected: {}", endRoom);
                level.getRoomsMap().put(endRoom.getPoint(), endRoom);
                level.setEnd(endRoom);
                level.getGrid()[endRoom.getPoint().getX()][endRoom.getPoint().getY()].setEmoji(getIcon(Optional.of(RoomType.END)));
            }
        } else {
            //corner case when second walker iterator failed to build path from crossroad
            //and attempts to build dead end in the same point
            deadEndSection.setDeadEnd(false);
            deadEndSection.setCrossroad(false);
            level.getDeadEnds().remove(deadEndSection);
        }
        return walkerBuilderIterator;
    }

    /**
     * Attempts to build next section of corridor
     * @param level being generated
     * @param walkerBuilderIterator active walker
     * @return optional of updated walker after successful path building
     * empty if unable to build path
     */
    private Optional<WalkerBuilderIterator> getNextSectionAndBuildPath(Level level, WalkerBuilderIterator walkerBuilderIterator) {
        log.debug("Building next section from x:{}, y:{}, current walker: {}", walkerBuilderIterator.getCurrentPoint().getPoint().getX(), walkerBuilderIterator.getCurrentPoint().getPoint().getY(), walkerBuilderIterator);
        int pathLength;
        val oldDirection = walkerBuilderIterator.getDirection();
        log.debug("Old direction: {}", oldDirection);
        val startSection = walkerBuilderIterator.getCurrentPoint();
        val directionOptional = getRandomValidDirection(walkerBuilderIterator, level);
        if (directionOptional.isEmpty()) {
            //if continuing path in any direction is not possible
            return Optional.empty();
        }
        val direction = directionOptional.get();
        log.debug("Calculating length in direction: {}", direction);
        val maxLengthInDirection = calculateMaxLengthInDirection(level.getGrid(), startSection, direction);
        if (maxLengthInDirection < level.getMinLength()) {
            //double-check: returns empty if can't build corridor of permitted length in selected direction
            return Optional.empty();
        }
        log.debug("Max length in {} direction is {}", direction, maxLengthInDirection);
        val max = min(level.getMaxLength(), maxLengthInDirection);
        pathLength = RandomUtil.getRandomInt(level.getMinLength(), max);
        if (pathLength >= level.getMinLength() && pathLength <= level.getMaxLength()) {
            log.debug("Random path length to walk: {}", pathLength);
            walkerBuilderIterator.setDirection(direction);
            //Building path in chosen direction and path length
            return Optional.of(buildPathInDirection(level, walkerBuilderIterator, pathLength));
        }
        return Optional.empty();
    }

    /**
     * Fills every grid with normal empty room with active walker
     * @param level being generated
     * @param walkerBuilderIterator active walker
     * @param pathLength length of corridor to build
     * @return updated walker
     */
    private WalkerBuilderIterator buildPathInDirection(Level level, WalkerBuilderIterator walkerBuilderIterator, int pathLength) {
        GridSection nextStep;
        Room nextRoom;
        Room previousRoom;
        for (int i = 0; i < pathLength; i++) {
            val nextPoint = getNextPointInDirection(walkerBuilderIterator.getCurrentPoint().getPoint(),
                    walkerBuilderIterator.getDirection());
            log.debug("Building next room [x:{}, y;{}]...", nextPoint.getX(), nextPoint.getY());
            nextRoom = buildRoom(nextPoint, level.getChatId());
            nextRoom = roomService.saveOrUpdateRoom(nextRoom);
            previousRoom = walkerBuilderIterator.getPreviousRoom();
            level.getRoomsMap().put(nextPoint, nextRoom);
            log.debug("Rooms count: {}", level.getRoomsMap().size());
            nextStep = buildNextStep(level.getGrid(), nextPoint, walkerBuilderIterator,
                    Objects.isNull(nextRoom.getRoomContent()) ?
                            Optional.empty() :
                            Optional.of(nextRoom.getRoomContent().getRoomType()));
            log.debug("Current path: {}", walkerBuilderIterator.getPathFromStart());
            previousRoom.addAdjacentRoom(walkerBuilderIterator.getDirection());
            roomService.saveOrUpdateRoom(previousRoom);
            nextRoom.addAdjacentRoom(getOppositeDirection(walkerBuilderIterator.getDirection()));
            roomService.saveOrUpdateRoom(nextRoom);
            previousRoom = nextRoom;

            walkerBuilderIterator.setCurrentPoint(nextStep);
            walkerBuilderIterator.setPreviousRoom(previousRoom);
        }
        log.debug("Current map state\n{}", printMap(level.getGrid()));
        return walkerBuilderIterator;
    }

    private GridSection buildNextStep(GridSection[][] grid, Point nextPoint, WalkerBuilderIterator walkerBuilderIterator, Optional<RoomType> roomType) {
        GridSection nextStep = grid[nextPoint.getX()][nextPoint.getY()];
        nextStep.setVisited(true);
        nextStep.setEmoji(getIcon(roomType));
        nextStep.setStepsFromStart(walkerBuilderIterator.getCurrentPoint().getStepsFromStart() + 1);
        grid[nextPoint.getX()][nextPoint.getY()] = nextStep;
        return nextStep;
    }

    /**
     * Generates clusters of generated room content
     * and distributes it to generated level rooms
     * @param level generated level
     * @param player current player
     */
    private void distributeRoomTypes(Level level, Player player) {
        log.debug("Distributing rooms content...");
        WalkerDistributeIterator walkerIterator;
        //generating clusters with room content
        var clusters = randomRoomTypeGenerator.generateClusters(level, player);
        randomRoomTypeGenerator.updateDeadEndsForDistribution(level, clusters);
        log.debug("Clusters generated for levelId:{}: {}", level.getChatId(), clusters);
        while (clusters.hasClusters() && clusters.hasDeadEnds()) {
            log.debug("Getting next segment...");
            //distributing content from end to start of cluster segment
            var currentSection = clusters.getNextDeadEnd();
            var currentSegment = clusters.getSegmentByDeadEnd(currentSection.getPoint());
            //initializing walker for distributing content
            walkerIterator = WalkerDistributeIterator.builder()
                    .currentRoom(level.getRoomByCoordinates(currentSection.getPoint()))
                    .segment(currentSegment)
                    .build();
            log.debug("Room distribute walker initialized: {}", walkerIterator);
            val cluster = clusters.getClusterBySegment(currentSegment);
            log.debug("Processing cluster - weight: {}, size: {}", cluster.getClusterWeight(), cluster.getRooms().size());
            distributeRoomTypesCluster(level, walkerIterator, cluster);
        }
        if (clusters.hasClusters()) {
            //processing last cluster containing path
            //from start to end of level separately
            log.debug("Processing last cluster...");
            var currentRoom = level.getEnd();
            var currentSegment = clusters.getMainSegment();
            walkerIterator = WalkerDistributeIterator.builder()
                    .currentRoom(currentRoom)
                    .segment(currentSegment)
                    .build();
            log.debug("Room distribute walker initialized: {}", walkerIterator);
            val cluster = clusters.getClusterBySegment(currentSegment);
            log.debug("Processing cluster - weight: {}, size: {}", cluster.getClusterWeight(), cluster.getRooms().size());
            distributeRoomTypesCluster(level, walkerIterator, cluster);
        }
    }

    /**
     * Distributes room content cluster to corresponding segment,
     * from dead end (end) to crossroad (start)
     * @param level generated level
     * @param walkerIterator active walker
     * @param cluster distributed cluster
     */
    private void distributeRoomTypesCluster(Level level, WalkerDistributeIterator walkerIterator,
                                            RoomTypesCluster cluster) {
        while (cluster.hasNextRoomToDistribute()) {
            val previousRoom = walkerIterator.getPreviousRoom();
            if (nonNull(walkerIterator.getCurrentRoom().getRoomContent()) &&
                    RoomType.END.equals(walkerIterator.getCurrentRoom().getRoomContent().getRoomType())) {
                //separately processing start of cluster with start to end of the level segment
                log.debug("Processing end room...");
                var currentRoom = walkerIterator.getCurrentRoom();
                walkerIterator.setCurrentRoom(currentRoom.getAdjacentRooms().entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                        .map(level::getRoomByCoordinates)
                        .findFirst().get());
                walkerIterator.setPreviousRoom(level.getEnd());
            } else {
                val currentRoom = walkerIterator.getCurrentRoom();
                val adjacentRoomsCount = toIntExact(currentRoom.getAdjacentRooms().entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .count());
                //since start point of cluster is either crossroad or start room we can determine each by
                // amount of adjacent rooms
                switch (adjacentRoomsCount) {
                    case 1 -> {
                        //if room has one entrance it either dead end (start of cluster) or
                        //start of the level (end of corresponding cluster)
                        //
                        //in first case, adding content from cluster to room
                        if (isNull(walkerIterator.getCurrentRoom().getRoomContent()) || !RoomType.START.equals(walkerIterator.getCurrentRoom().getRoomContent().getRoomType())) {
                            log.debug("Processing dead end room...");
                            setRoomContent(level.getGrid(), walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                            val nextRoom = walkerIterator.getCurrentRoom().getAdjacentRooms().entrySet().stream()
                                    .filter(Map.Entry::getValue)
                                    .map(Map.Entry::getKey)
                                    .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                                    .map(level::getRoomByCoordinates)
                                    .findFirst().get();
                            walkerIterator.setPreviousRoom(walkerIterator.getCurrentRoom());
                            walkerIterator.setCurrentRoom(nextRoom);
                        }
                        //no need to do anything with start room
                    }
                    case 2 -> {
                        //if room has two entrances simply go on through different door walker entered
                        //leaving content from cluster behind
                        log.debug("Processing room...");
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
                    case 3 -> {
                        log.debug("Processing crossroad...");
                        //if room has three entrances than it's a crossroad
                        //if it's start of current cluster segment, leave it alone (to another walker)
                        //else, fill room with content and go to room with descending path from level start
                        //(here goes that 'another' walker)
                        if (!walkerIterator.getSegment().getEnd().getPoint().equals(walkerIterator.getCurrentRoom().getPoint())) {
                            setRoomContent(level.getGrid(), walkerIterator.getCurrentRoom(), cluster.getNextRoom());
                            val nextRoom = walkerIterator.getCurrentRoom().getAdjacentRooms().entrySet().stream()
                                    .filter(Map.Entry::getValue)
                                    .map(Map.Entry::getKey)
                                    .map(direction -> getNextPointInDirection(currentRoom.getPoint(), direction))
                                    .filter(point -> !point.equals(previousRoom.getPoint()))
                                    .map(point -> level.getGrid()[point.getX()][point.getY()])
                                    .sorted(Comparator.comparing(GridSection::getStepsFromStart))
                                    .map(GridSection::getPoint)
                                    .map(level::getRoomByCoordinates)
                                    .findFirst().get();
                            walkerIterator.setPreviousRoom(walkerIterator.getCurrentRoom());
                            walkerIterator.setCurrentRoom(nextRoom);
                        }
                    }
                }
            }
        }
    }

    private void setRoomContent(GridSection[][] grid, Room room, RoomContent roomContent) {
        log.debug("Setting type {} to room [x:{}, y:{}]", roomContent.getRoomType(), room.getPoint().getX(), room.getPoint().getY());
        grid[room.getPoint().getX()][room.getPoint().getY()].setEmoji(getIcon(Optional.of(roomContent.getRoomType())));
        room.setRoomContent(roomContent);
        roomService.saveOrUpdateRoom(room);
        log.debug("Current map state\n{}", printMap(grid));
    }

    private Room buildRoom(Point point, Long chatId) {
        val room = new Room();
        room.setChatId(chatId);
        room.setPoint(point);
        room.setRoomContent(new NormalRoom());
        return room;
    }

    private Room buildStartRoom(Point point, Long chatId) {
        return new Room(point, chatId, new StartRoom());
    }

    private Room buildEndRoom(Point point, Long chatId) {
        return new Room(point, chatId, new EndRoom());
    }

    private Integer calculateMaxLength(Integer gridSize) {
        return (int) (gridSize * generationProperties.getLevel().getMaxLengthRatio());
    }

    private Integer calculateMinLength(Integer gridSize) {
        val properties = generationProperties.getLevel();
        return (int) (gridSize * properties.getMinLengthRatio()) < properties.getMinLength() ? properties.getMinLength() :
                (int) (gridSize * properties.getMinLengthRatio());
    }

    private Integer calculateGridSize(Integer levelNumber) {
        val properties = generationProperties.getLevel();
        val increments = (levelNumber - 1) / properties.getIncrementStep();
        return properties.getLevelOneGridSize() + increments * properties.getGridSizeIncrement();
    }
    private GridSection setStartSection(GridSection[][] grid, Point startPoint) {
        val startSection = grid[startPoint.getX()][startPoint.getY()];
        startSection.setStepsFromStart(0);
        startSection.setEmoji(getIcon(Optional.of(RoomType.START)));
        startSection.setVisited(true);
        return startSection;
    }
}
