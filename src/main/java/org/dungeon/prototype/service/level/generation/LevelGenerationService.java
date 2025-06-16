package org.dungeon.prototype.service.level.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.async.AsyncJobHandler;
import org.dungeon.prototype.async.TaskType;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.level.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.level.generation.LevelGridCluster;
import org.dungeon.prototype.model.level.generation.NextRoomDto;
import org.dungeon.prototype.model.level.ui.LevelMap;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.room.content.EndRoom;
import org.dungeon.prototype.model.room.content.ItemsRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.room.content.StartRoom;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.properties.GenerationProperties;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.room.generation.room.content.RoomContentGenerationService;
import org.dungeon.prototype.service.weight.WeightCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.abs;
import static org.apache.commons.math3.util.FastMath.max;
import static org.dungeon.prototype.util.LevelUtil.generateEmptyMapGrid;
import static org.dungeon.prototype.util.LevelUtil.getAdjacentSections;
import static org.dungeon.prototype.util.LevelUtil.getAdjacentSectionsInCluster;
import static org.dungeon.prototype.util.LevelUtil.getIcon;
import static org.dungeon.prototype.util.LevelUtil.printMapGridToLogs;
import static org.dungeon.prototype.util.LevelUtil.printMapToLogs;
import static org.dungeon.prototype.util.LevelUtil.setMutualAdjacency;
import static org.dungeon.prototype.util.RandomUtil.getDeadEndRouteRoomType;
import static org.dungeon.prototype.util.RandomUtil.getRandomClusterConnectionRoomType;
import static org.dungeon.prototype.util.RandomUtil.getRandomInt;
import static org.dungeon.prototype.util.RandomUtil.getRandomRoomType;

@Slf4j
@Service
public class LevelGenerationService {
    @Autowired
    private RoomContentGenerationService roomContentGenerationService;
    @Autowired
    private WeightCalculationService weightCalculationService;
    @Autowired
    private AsyncJobHandler asyncJobHandler;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private GenerationProperties generationProperties;

    public Level generateAndPopulateLevel(Long chatId, Integer levelNumber) {
        var levelMap = generateLevelMap(chatId, levelNumber);
        log.debug("Generated level map\n{}", printMapGridToLogs(levelMap.getGrid()));
        val futureLevel = asyncJobHandler.submitMapPopulationTask(() ->
                populateLevel(chatId, levelNumber, levelMap), TaskType.LEVEL_GENERATION, chatId);
        try {
            return futureLevel.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DungeonPrototypeException(e.getMessage());
        }
    }


    /**
     * Generates level map grid with clusters and connection points
     * and no content
     *
     * @param chatId      id of player's chat
     * @param levelNumber number of level
     * @return generated level
     */
    public Level generateLevelMap(long chatId, int levelNumber) {
        log.info("Generating level {}", levelNumber);
        //initializing level and calculating basic attributes
        int gridSize = calculateGridSize(levelNumber);
        var level = new Level();
        level.setChatId(chatId);
        level.setNumber(levelNumber);
        log.info("Grid size {}", gridSize);

        val clusterConnectionPoints =
                generateClusterConnectionPoints(new Point(0, 0), new Point(gridSize - 1, gridSize - 1));

        log.info("Cluster connection points list: {}", clusterConnectionPoints);

        val clusters = clusterConnectionPoints
                .stream()
                .filter(point -> !clusterConnectionPoints.getFirst().equals(point))
                .map(point -> new LevelGridCluster(clusterConnectionPoints
                        .get(clusterConnectionPoints.indexOf(point) - 1), point))
                .collect(Collectors.toMap(LevelGridCluster::getId, Function.identity()));

        //empty level grid will be filled with rooms during generation
        GridSection[][] grid = generateEmptyMapGrid(gridSize);
        initConnectionSections(grid, clusterConnectionPoints);
        level.setClusterConnectionPoints(clusterConnectionPoints);

        try (var clusterScope = asyncJobHandler.openClusterScope(chatId)) {
            var tasks = clusters.values().stream()
                    .collect(Collectors.toMap(LevelGridCluster::getId,
                            cluster -> asyncJobHandler.submitClusterGenerationTask(
                                    clusterScope,
                                    () -> generateGridSectionWithRetries(cluster),
                                    TaskType.LEVEL_GENERATION,
                                    chatId,
                                    cluster.getId())));
            clusterScope.join();
            tasks.forEach((id, subtask) -> {
                try {
                    var result = clusterScope.getResult(subtask);
                    var clusterData = clusters.get(result.clusterId());
                    copyGridSection(grid,
                            clusterData.getStartConnectionPoint(),
                            clusterData.getEndConnectionPoint(),
                            result.clusterGrid());
                } catch (Exception e) {
                    log.warn("Cluster generation failed for chatId {}: {}", chatId, e.getMessage());
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Cluster generation interrupted for chatId: {}", chatId);
        }

        log.info("All clusters generated, current grid state:\n{}", printMapGridToLogs(grid));

        level.setGrid(grid);
        level.setClusters(clusters.values().stream()
                .collect(Collectors.toMap(LevelGridCluster::getEndConnectionPoint, Function.identity())));

        return level;
    }

    private GridSection[][] generateGridSectionWithRetries(LevelGridCluster cluster) {
        int attempts = 0;
        int maxAttempts = generationProperties.getLevel().getClusterGenerationRetries();
        while (true) {
            try {
                cluster.reset();
                cluster.initializeWalkers();
                return generateGridSection(cluster);
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    log.warn("Cluster {} generation failed after {} attempts: {}", cluster.getId(), attempts, e.getMessage());
                    throw e;
                }
                log.warn("Cluster {} generation attempt {} failed: {}. Retrying...", cluster.getId(), attempts, e.getMessage());
            }
        }
    }

    private GridSection[][] generateGridSection(LevelGridCluster cluster) {
        log.info("Generating cluster {} with start point {} and end point {}",
                cluster.getId(), cluster.getStartConnectionPoint(), cluster.getEndConnectionPoint());
        GridSection[][] clusterGrid = generateEmptyMapGrid(cluster.getStartConnectionPoint(), cluster.getEndConnectionPoint());
        log.debug("Empty cluster grid:\n {}", printMapGridToLogs(clusterGrid));
        List<WalkerBuilder> walkerBuilders = cluster.getWalkers();
        while (!walkerBuilders.isEmpty()) {
            for (WalkerBuilder walker : walkerBuilders) {
                log.debug("Current walker: {}", walker);
                clusterGrid = walker.nextStep(clusterGrid);
                log.debug("Current cluster grid state\n{}", printMapGridToLogs(clusterGrid));
            }
            walkerBuilders.removeIf(WalkerBuilder::isStopped);
            log.debug("Current cluster walkers: {}", walkerBuilders);
        }
        if (cluster.hasNegativeRooms()) {
            log.info("Processing negative rooms of cluster {}", cluster.getId());
            GridSection endSection = clusterGrid[clusterGrid.length - 1][clusterGrid[0].length - 1];
            processNegativeSections(clusterGrid, cluster, endSection);
        }
        if (cluster.hasDeadEnds()) {
            log.info("Processing dead ends of cluster {}...", cluster.getId());
            processDeadEnds(clusterGrid, cluster);
        }
        return clusterGrid;
    }

    private void copyGridSection(GridSection[][] grid, Point startConnectionPoint, Point endConnectionPoint, GridSection[][] gridSection) {
        log.info("Copying grid section from {} to {}",
                startConnectionPoint, endConnectionPoint);
        log.debug("Grid section\n{}", printMapGridToLogs(gridSection));
        log.debug("Grid before copying\n{}", printMapGridToLogs(grid));
        IntStream.range(startConnectionPoint.getX(), endConnectionPoint.getX() + 1)
                .forEach(x -> IntStream.range(startConnectionPoint.getY(), endConnectionPoint.getY() + 1)
                        .forEach(y -> {
                            if (!isStartOrEnd(x, y, startConnectionPoint, endConnectionPoint)) {
                                grid[x][y] = gridSection[x - startConnectionPoint.getX()][y - startConnectionPoint.getY()];
                                grid[x][y].setPoint(new Point(x, y));
                            }
                        }));
        log.debug("Grid after copying\n{}", printMapGridToLogs(grid));
    }

    /**
     * Generates level and fills with content
     *
     * @param chatId      id of player's chat
     * @param levelNumber number of level
     * @param level       generated level map
     * @return generated level
     */
    public Level populateLevel(long chatId, int levelNumber, Level level) {
        val player = playerService.getPlayer(chatId);
        log.info("Generating level {}", levelNumber);

        Set<String> usedItemIds = new HashSet<>();
        Map<Point, Room> roomsMap = new HashMap<>();
        double levelDensity = level.getClusters().values().stream().mapToInt(LevelGridCluster::getSize).sum() / (double) (level.getGrid().length * level.getGrid().length);
        populatingClusterConnectionPoints(chatId, player, level, usedItemIds, roomsMap, levelDensity);

        for (LevelGridCluster cluster : level.getClusters().values()) {
            Point clusterEndPoint = cluster.getEndConnectionPoint();
            log.info("Processing cluster endSection: {}", clusterEndPoint);
            int deadEndsRouteStepsCount = 0;
            if (cluster.hasDeadEnds()) {
                deadEndsRouteStepsCount = populateDeadEnds(chatId, player, level.getGrid(), cluster, roomsMap, usedItemIds);
            }
            var adjacentSections = getAdjacentSectionsInCluster(clusterEndPoint, level.getGrid(), cluster).stream()
                    .filter(section -> section.getStepsFromStart() > 0)
                    .collect(Collectors.toCollection(HashSet::new));
            var mainPathStart = adjacentSections.stream()
                    .max(Comparator.comparing(GridSection::getStepsFromStart))
                    .orElseGet(GridSection::new);
            log.info("Main path start: {}", mainPathStart);
            val mainPathWalker = WalkerDistributor.builder()
                    .chatId(chatId)
                    .cluster(cluster)
                    .currentSection(level.getGrid()[clusterEndPoint.getX()][clusterEndPoint.getY()])
                    .runSubWalkerOnRouteFork(true)
                    .previousRoom(roomsMap.get(clusterEndPoint))
                    .currentStep(mainPathStart.getStepsFromStart())
                    .status(WalkerDistributor.Status.RUNNING)
                    .mainPathLength(mainPathStart.getStepsFromStart())
                    .totalSteps(cluster.getSize() - deadEndsRouteStepsCount)
                    .build();
            while (mainPathWalker.isRunning()) {
                log.info("Main cluster walker - id: {}, status: {}, current step: {}, current point: {}",
                        mainPathWalker.getId(), mainPathWalker.getStatus(), mainPathWalker.getCurrentStep(),
                        mainPathWalker.getCurrentSection().getPoint());
                log.info("Main path walker next step...");
                val nextRoom = mainPathWalker.nextStep(level.getGrid(), roomsMap);
                if (nonNull(nextRoom)) {
                    populateRoom(chatId, player, level.getGrid(), usedItemIds, roomsMap, cluster, nextRoom);
                }
            }

            while (!mainPathWalker.getSubWalkers().isEmpty()) {
                log.info("Processing running sub-walkers...");
                for (WalkerDistributor walker : mainPathWalker.getSubWalkers()) {
                    log.info("Sub-walker - id: {}, running: {}, current step: {}, current point: {}",
                            walker.getId(), walker.isRunning(), walker.getCurrentStep(),
                            walker.getCurrentSection().getPoint());
                    NextRoomDto nextRoom;
                    if (walker.isRunning()) {
                        nextRoom = walker.nextStep(level.getGrid(), roomsMap);
                        if (nonNull(nextRoom)) {
                            populateRoom(chatId, player, level.getGrid(), usedItemIds, roomsMap, cluster, nextRoom);
                        }
                    } else {
                        if (walker.isWaiting()) {
                            while (!walker.getSubWalkers().isEmpty()) {
                                for (WalkerDistributor walkerDistributor : walker.getSubWalkers()) {
                                    nextRoom = walkerDistributor.nextStep(level.getGrid(), roomsMap);
                                    if (nonNull(nextRoom)) {
                                        populateRoom(chatId, player, level.getGrid(), usedItemIds, roomsMap, cluster, nextRoom);
                                    }
                                }
                                walker.getSubWalkers().removeIf(WalkerDistributor::finished);
                            }
                            walker.setStatus(WalkerDistributor.Status.FINISHED);
                        }
                    }
                }
                mainPathWalker.getSubWalkers().removeIf(WalkerDistributor::finished);
                log.debug("Current map state\n{}", printMapToLogs(level.getGrid(), roomsMap));
            }
        }
        level.setRoomsMap(roomsMap);
        level.setLevelMap(new LevelMap(setStartSection(level.getGrid(), level.getClusterConnectionPoints().getFirst())));
        log.debug("Current map state\n{}", printMapToLogs(level.getGrid(), roomsMap));
        log.debug("Current grid state\n{}", printMapGridToLogs(level.getGrid()));
        val direction = level.getRoomsMap().get(level.getStart()).getAdjacentRooms().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()) && entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        player.setDirection(direction);
        player.setCurrentRoom(level.getStart());
        player.setCurrentRoomId(level.getRoomsMap().get(level.getStart()).getId());
        playerService.updatePlayer(player);

        return level;
    }

    private void populatingClusterConnectionPoints(long chatId, Player player, Level level, Set<String> usedItemIds, Map<Point, Room> roomsMap, double levelDensity) {
        log.info("Populating connection point rooms with content...");
        for (int i = 0; i < level.getClusterConnectionPoints().size(); i++) {
            if (i == 0) {
                Room start = new Room(level.getClusterConnectionPoints().get(i), chatId, new StartRoom());
                level.setStart(start.getPoint());
                roomsMap.put(start.getPoint(), start);
                log.debug("Current map state\n{}", printMapToLogs(level.getGrid(), roomsMap));
                continue;
            }
            if (i == level.getClusterConnectionPoints().size() - 1) {
                Room end = new Room(level.getClusterConnectionPoints().get(i), chatId, new EndRoom());
                level.setEnd(end.getPoint());
                roomsMap.put(end.getPoint(), end);
                Weight expectedWeight = weightCalculationService.getExpectedClusterConnectionPointWeight(player.getWeight(),
                        player.getWeight().toVector().getNorm() / levelDensity,
                        i, level.getClusterConnectionPoints().size());
                level.getClusters().get(level.getClusterConnectionPoints().get(i)).setClusterExpectedWeight(expectedWeight);
                continue;
            }
            var currentSection = level.getGrid()[level.getClusterConnectionPoints().get(i).getX()][level.getClusterConnectionPoints().get(i).getY()];
            log.info("Current section:{}", currentSection);
            Weight expectedWeight = weightCalculationService.getExpectedClusterConnectionPointWeight(player.getWeight(),
                    player.getWeight().toVector().getNorm() / levelDensity,
                    i, level.getClusterConnectionPoints().size());
            log.info("Expected weight norm: {}, expected weight: {}", expectedWeight.toVector().getNorm(), expectedWeight);
            val roomType = getRandomClusterConnectionRoomType(expectedWeight, i, level.getClusterConnectionPoints().size());
            val roomContent = roomContentGenerationService.getNextRoomContent(expectedWeight, chatId, roomType, usedItemIds);
            if (roomContent instanceof ItemsRoom itemsRoom) {
                usedItemIds.addAll(itemsRoom.getItems().stream().map(Item::getId).collect(Collectors.toSet()));
            }
            level.getClusters().get(currentSection.getPoint()).setClusterExpectedWeight(roomContent.getRoomContentWeight());
            Room room = buildRoom(currentSection, chatId, roomContent);
            roomsMap.put(room.getPoint(), room);
            log.debug("Current map state\n{}", printMapToLogs(level.getGrid(), roomsMap));
        }
    }

    private void populateRoom(long chatId, Player player,
                              GridSection[][] grid, Set<String> usedItemIds,
                              Map<Point, Room> roomsMap, LevelGridCluster cluster,
                              NextRoomDto nextRoom) {
        log.info("Populating room {}...", nextRoom.getSection().getPoint());
        Weight expectedWeight = weightCalculationService.getExpectedWeigth(cluster.getClusterExpectedWeight(),
                player.getWeight().toVector().getNorm() / cluster.getDensity(),
                nextRoom.getCurrentStep(),
                nextRoom.getTotalSteps());
        log.info("Expected weight: {}", expectedWeight);
        val roomType = getRandomRoomType(expectedWeight, nextRoom.getCurrentStep(), nextRoom.getTotalSteps());
        log.info("Room type: {}", roomType);
        RoomContent roomContent = roomContentGenerationService.getNextRoomContent(expectedWeight, chatId, roomType, usedItemIds);
        if (roomContent instanceof ItemsRoom itemsRoom) {
            usedItemIds.addAll(itemsRoom.getItems().stream().map(Item::getId).collect(Collectors.toSet()));
        }
        Room room = nextRoom.getRoom();
        setRoomContent(grid, room, roomContent, roomsMap);
        roomsMap.put(room.getPoint(), room);
        if (nextRoom.getCurrentStep() == 0) {
            setMutualAdjacency(roomsMap.get(cluster.getStartConnectionPoint()), room);
        }
        log.info("Current map state\n{}", printMapToLogs(grid, roomsMap));
    }

    private int populateDeadEnds(long chatId, Player player, GridSection[][] grid, LevelGridCluster cluster, Map<Point, Room> roomsMap, Set<String> usedItemIds) {
        return cluster.getDeadEnds().stream()
                .mapToInt(section -> populateDeadEnd(chatId, player, grid, section, cluster, roomsMap, usedItemIds))
                .sum();
    }

    private int populateDeadEnd(long chatId, Player player, GridSection[][] grid, GridSection start, LevelGridCluster cluster, Map<Point, Room> roomsMap, Set<String> usedItemIds) {
        val roomContent = roomContentGenerationService.getSpecialTreasure(chatId, player.getAttributes().get(PlayerAttribute.LUCK), usedItemIds);
        usedItemIds.addAll(roomContent.getItems().stream().map(Item::getId).collect(Collectors.toSet()));
        val rewardWeight = roomContent.getRoomContentWeight();
        var room = buildRoom(start, chatId, roomContent);
        roomsMap.put(room.getPoint(), room);
        val adjacentSections = getAdjacentSectionsInCluster(start.getPoint(), grid, cluster).stream()
                .filter(section -> section.getStepsFromStart() == start.getStepsFromStart() - 1)
                .toList();
        ArrayList<WalkerDistributor> deadEndPopulatingWalkers;
        int totalRooms = adjacentSections.stream().mapToInt(GridSection::getStepsFromStart).sum();
        Weight lastAddedWeight = null;
        if (adjacentSections.size() > 1) {
            deadEndPopulatingWalkers = Stream.of(WalkerDistributor.builder()
                            .chatId(chatId)
                            .previousRoom(room)
                            .runSubWalkerOnRouteFork(false)
                            .status(WalkerDistributor.Status.RUNNING)
                            .cluster(cluster)
                            .totalSteps(totalRooms)
                            .currentSection(start)
                            .currentStep(adjacentSections.getFirst().getStepsFromStart())
                            .build(),
                    WalkerDistributor.builder()
                            .chatId(chatId)
                            .previousRoom(room)
                            .runSubWalkerOnRouteFork(false)
                            .status(WalkerDistributor.Status.RUNNING)
                            .cluster(cluster)
                            .totalSteps(totalRooms)
                            .currentSection(start)
                            .currentStep(adjacentSections.getLast().getStepsFromStart())
                            .build()
            ).collect(Collectors.toCollection(ArrayList::new));
        } else {
            deadEndPopulatingWalkers = Stream.of(WalkerDistributor.builder()
                    .chatId(chatId)
                    .previousRoom(room)
                    .runSubWalkerOnRouteFork(false)
                    .status(WalkerDistributor.Status.RUNNING)
                    .cluster(cluster)
                    .totalSteps(totalRooms)
                    .currentSection(start)
                    .currentStep(adjacentSections.getFirst().getStepsFromStart())
                    .build()).collect(Collectors.toCollection(ArrayList::new));
        }
        while (!deadEndPopulatingWalkers.isEmpty()) {
            totalRooms += deadEndPopulatingWalkers.stream().mapToInt(WalkerDistributor::getTotalSteps).sum();
            deadEndPopulatingWalkers.removeIf(walkerDistributor -> !walkerDistributor.isRunning());
            for (WalkerDistributor walkerDistributor : deadEndPopulatingWalkers) {
                val next = walkerDistributor.nextStep(grid, roomsMap);
                if (nonNull(next)) {
                    val nextExpectedWeight = weightCalculationService.getExpectedDeadEndRouteWeight(
                            isNull(lastAddedWeight) ? rewardWeight : lastAddedWeight,
                            rewardWeight.toVector().getNorm(),
                            next.getCurrentStep());
                    val nextRoomType = getDeadEndRouteRoomType(nextExpectedWeight, next.getCurrentStep(), cluster.getDensity());
                    val nextRoomContent = roomContentGenerationService.getNextRoomContent(nextExpectedWeight, chatId, nextRoomType, usedItemIds);
                    if (nextRoomContent instanceof ItemsRoom itemsRoom) {
                        usedItemIds.addAll(itemsRoom.getItems().stream().map(Item::getId).collect(Collectors.toSet()));
                    }
                    val nextRoom = next.getRoom();
                    lastAddedWeight = nextRoomContent.getRoomContentWeight();
                    setRoomContent(grid, nextRoom, nextRoomContent, roomsMap);
                    roomsMap.put(room.getPoint(), room);
                    if (next.getCurrentStep() == 0) {
                        setMutualAdjacency(roomsMap.get(cluster.getStartConnectionPoint()), room);
                    }
                }
            }
        }
        return totalRooms;
    }

    private void initConnectionSections(GridSection[][] grid, LinkedList<Point> clusterConnectionPoints) {
        log.info("Initializing connection sections...");
        for (Point point : clusterConnectionPoints) {
            val section = new GridSection(point.getX(), point.getY());
            section.setConnectionPoint(true);
            grid[point.getX()][point.getY()] = section;
        }
    }

    private void processDeadEnds(GridSection[][] grid, LevelGridCluster cluster) {
        log.info("Processing deadEnds of cluster {}", cluster);
        List<GridSection> processedDeadEnds = new ArrayList<>();
        cluster.getDeadEnds().removeIf(deadEnd -> {
            val adjacentSections = getAdjacentSections(deadEnd.getPoint(), grid);
            if (adjacentSections.stream()
                    .anyMatch(section -> section.getStepsFromStart() == deadEnd.getStepsFromStart() + 1) &&
                    adjacentSections.stream()
                            .anyMatch(section -> section.getStepsFromStart() == deadEnd.getStepsFromStart() - 1)) {
                deadEnd.setDeadEnd(false);
                return true;
            } else {
                var oldPath = deadEnd.getStepsFromStart();
                val expectedPath = getCrossroadPairMiddlePath(adjacentSections.stream().toList(), oldPath);
                if (expectedPath.isPresent()) {
                    int path = expectedPath.get();
                    if (path == oldPath) {
                        deadEnd.setDeadEnd(false);
                        return true;
                    }
                    if (deadEnd.getStepsFromStart() > path) {
                        var currentSection = deadEnd;
                        currentSection.setStepsFromStart(path - 1);
                        while (path <= oldPath) {
                            oldPath--;
                            GridSection found = currentSection;
                            for (GridSection section : getAdjacentSections(currentSection.getPoint(), grid)) {
                                if (oldPath == section.getStepsFromStart()) {
                                    found = section;
                                    break;
                                }
                            }
                            currentSection = found;
                            currentSection.setStepsFromStart(path);
                            path++;
                        }
                        processedDeadEnds.add(currentSection);
                        deadEnd.setDeadEnd(false);
                        return true;
                    }
                    return false;
                }
                return false;
            }
        });
        log.info("Processed dead ends:{}", processedDeadEnds);
        processedDeadEnds.removeIf(deadEnd -> {
            val adjacentSections = getAdjacentSections(deadEnd.getPoint(), grid);
            if (adjacentSections.stream()
                    .anyMatch(section -> section.getStepsFromStart() == deadEnd.getStepsFromStart() + 1) &&
                    adjacentSections.stream()
                            .anyMatch(section -> section.getStepsFromStart() == deadEnd.getStepsFromStart() - 1)) {
                deadEnd.setDeadEnd(false);
                return true;
            } else {
                deadEnd.setDeadEnd(true);
                return false;
            }
        });
        log.info("Processed dead ends filtered out: {}", processedDeadEnds);
        cluster.addDeadEnds(processedDeadEnds);
        log.debug("Cluster data:{}", cluster);
        log.debug("Current grid state\n{}", printMapGridToLogs(grid));
    }

    private boolean isStartOrEnd(int x, int y, Point start, Point end) {
        return start.equals(new Point(x, y)) || end.equals(new Point(x, y));
    }

    private Optional<Integer> getCrossroadPairMiddlePath(List<GridSection> adjacentSections, int expectedPath) {
        List<Integer> availableOptions = new ArrayList<>();
        for (int i = 0; i < adjacentSections.size(); i++) {
            for (int j = i + 1; j < adjacentSections.size(); j++) {
                int diff = Math.abs(adjacentSections.get(i).getStepsFromStart() - adjacentSections.get(j).getStepsFromStart());
                if (diff == 2) {
                    if ((max(adjacentSections.get(i).getStepsFromStart(), adjacentSections.get(j).getStepsFromStart()) == expectedPath + 1)) {
                        return Optional.of(expectedPath);
                    } else {
                        availableOptions.add(max(adjacentSections.get(i).getStepsFromStart(), adjacentSections.get(j).getStepsFromStart()));
                    }
                }
            }
        }
        if (availableOptions.isEmpty()) {
            return adjacentSections.stream()
                    .filter(section -> section.getStepsFromStart() < expectedPath - 1)
                    .max(Comparator.comparing(GridSection::getStepsFromStart))
                    .map(GridSection::getStepsFromStart);
        } else {
            return availableOptions.stream()
                    .filter(number -> number < expectedPath - 1)
                    .max(Integer::compareTo);
        }
    }

    private void processNegativeSections(GridSection[][] grid, LevelGridCluster cluster, GridSection currentSection) {
        val adjacentSections = getAdjacentSections(currentSection.getPoint(), grid);
        var stepsFromStart = currentSection.getStepsFromStart();
        val negativeAdjacentSections = adjacentSections.stream()
                .filter(section -> section.getStepsFromStart() < 0)
                .collect(Collectors.toSet());
        if (!negativeAdjacentSections.isEmpty()) {
            negativeAdjacentSections
                    .forEach(section -> processNegativeBranch(section, stepsFromStart, grid, cluster));
        }
        if (adjacentSections.stream().filter(section -> section.getStepsFromStart() > 0).count() < 3) {
            adjacentSections.stream()
                    .filter(section -> section.getStepsFromStart() > 0)
                    .filter(section -> !currentSection.getPoint().equals(section.getPoint()))
                    .findFirst()
                    .ifPresent(section ->
                            processNegativeSections(grid, cluster, section));
        }
        log.debug("Current grid state\n{}", printMapGridToLogs(grid));
    }

    private void processNegativeBranch(GridSection section, int stepsFromStart, GridSection[][] grid, LevelGridCluster cluster) {
        log.info("Processing negative branch of section {}, steps from start: {}", section, stepsFromStart);
        Optional<GridSection> currentSection = Optional.of(section);
        Optional<GridSection> lastSection = Optional.empty();
        int counter = 0;
        while (currentSection.isPresent()) {
            lastSection = currentSection;
            Optional<GridSection> nextSection = Optional.empty();
            for (GridSection adjacentSection : getAdjacentSections(currentSection.get().getPoint(), grid)) {
                if (adjacentSection.getStepsFromStart() == currentSection.get().getStepsFromStart() - 1) {
                    nextSection = Optional.of(adjacentSection);
                    counter++;
                    break;
                }
            }
            currentSection = nextSection;
        }

        GridSection negativeDeadEnd = lastSection.get();
        negativeDeadEnd.setDeadEnd(true);
        cluster.addDeadEnd(negativeDeadEnd);
        currentSection = Optional.of(negativeDeadEnd);
        while (currentSection.isPresent()) {
            currentSection.get().setStepsFromStart(stepsFromStart + counter + 1);
            Optional<GridSection> nextSection = Optional.empty();
            for (GridSection adjacentSection : getAdjacentSectionsInCluster(currentSection.get().getPoint(), grid, cluster)) {
                if (adjacentSection.getStepsFromStart() < 0) {
                    adjacentSection.setStepsFromStart(stepsFromStart + counter);
                    cluster.decrementNegativeRoomsCount();
                    nextSection = Optional.of(adjacentSection);
                    break;
                }
            }
            counter--;
            currentSection = nextSection;
            log.debug("Current grid state\n{}", printMapGridToLogs(grid));
        }
    }


    private LinkedList<Point> generateClusterConnectionPoints(Point start, Point end) {
        log.info("Generating cluster connection points...");
        LinkedList<Point> clusterConnectingPoints = new LinkedList<>();
        clusterConnectingPoints.add(start);
        clusterConnectingPoints.add(end);

        Map<Integer, Point> points;
        do {
            points = new HashMap<>();
            for (int i = 0; i < clusterConnectingPoints.size() - 1; i++) {
                val nextPoint = getClustersConnectionPoint(clusterConnectingPoints.get(i), clusterConnectingPoints.get(i + 1));
                if (nextPoint.isPresent()) {
                    log.info("Adding point {} to position {}", nextPoint.get(), i + 1);
                    points.put(i + 1, nextPoint.get());
                }
            }
            log.info("Points to add: {}", points);
            points.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())).forEach(entry -> {
                        clusterConnectingPoints.add(entry.getKey(), entry.getValue());
                        log.info("Point {} added to position {}", entry.getValue(), entry.getKey());
                    }
            );
        } while (!points.isEmpty());
        return clusterConnectingPoints;
    }

    private Optional<Point> getClustersConnectionPoint(Point start, Point end) {
        log.info("Calculating connection point for start: {}, end: {}", start, end);
        if ((abs(start.getX() - end.getX()) > 4) && (abs(start.getY() - end.getY()) > 4)) {
            int x = getNextConnectionPointCoord(start.getX(), end.getX());
            log.info("x coordinate of next connection point: {}", x);
            int y = getNextConnectionPointCoord(start.getY(), end.getY());
            log.info("y coordinate of next connection point: {}", y);
            return Optional.of(new Point(x, y));
        }
        log.info("Cluster is too small to divide, no point added");
        return Optional.empty();
    }

    private int getNextConnectionPointCoord(int start, int end) {
        int center = (end - start) / 2 + start;
        log.info("Center: {}", center);
        int rangeStart = center - 1;
        int rangeEnd;
        if (abs(end - start) % 2 == 0) {
            rangeEnd = center + 1;
        } else {
            rangeEnd = center + 2;
        }
        log.info("Generating random number between {} and {}", rangeStart, rangeEnd);
        return getRandomInt(rangeStart, rangeEnd);
    }

    private void setRoomContent(GridSection[][] grid, Room room, RoomContent roomContent, Map<Point, Room> roomsMap) {
        log.info("Setting type {} to room [x:{}, y:{}]", roomContent.getRoomType(), room.getPoint().getX(), room.getPoint().getY());
        grid[room.getPoint().getX()][room.getPoint().getY()].setEmoji(getIcon(Optional.of(roomContent.getRoomType())));
        room.setRoomContent(roomContent);
        log.debug("Current map state\n{}", printMapToLogs(grid, roomsMap));
    }

    private Room buildRoom(GridSection section, Long chatId, RoomContent roomContent) {
        val room = new Room();
        room.setChatId(chatId);
        section.setEmoji(getIcon(Optional.ofNullable(roomContent.getRoomType())));
        room.setPoint(section.getPoint());
        room.setRoomContent(roomContent);
        return room;
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
        return startSection;
    }
}
