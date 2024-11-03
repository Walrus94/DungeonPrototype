package org.dungeon.prototype.service.level.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.level.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.level.generation.LevelGridCluster;
import org.dungeon.prototype.model.level.generation.NextRoomDto;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.*;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.properties.GenerationProperties;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.service.room.generation.room.content.RoomContentGenerationService;
import org.dungeon.prototype.service.weight.WeightCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.math3.util.FastMath.*;
import static org.dungeon.prototype.util.LevelUtil.*;
import static org.dungeon.prototype.util.RandomUtil.*;

@Slf4j
@Service
public class LevelGenerationService {

    @Value("${generation.level.dead-end-length-threshold}")
    private int deadEndLengthThreshold;
    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomContentGenerationService roomContentGenerationService;
    @Autowired
    private WeightCalculationService weightCalculationService;
    @Autowired
    private GenerationProperties generationProperties;

    /**
     * Generates level and fills with content
     *
     * @param chatId      id of player's chat
     * @param player      player starting level
     * @param levelNumber number of level
     * @return generated level
     */
    public Level generateLevel(long chatId, Player player, int levelNumber) {
        log.debug("Generating level {}", levelNumber);
        //initializing level and calculating basic attributes
        val gridSize = calculateGridSize(levelNumber);
        var level = new Level();
        level.setChatId(chatId);
        level.setNumber(levelNumber);
        log.debug("Grid size {}", gridSize);

        val clusterConnectionPoints =
                generateClusterConnectionPoints(new Point(0, 0), new Point(gridSize - 1, gridSize - 1));

        log.debug("Cluster connection points list: {}", clusterConnectionPoints);

        val clusters = clusterConnectionPoints
                .stream()
                .filter(point -> !clusterConnectionPoints.getFirst().equals(point))
                .collect(Collectors.toMap(Function.identity(), point -> new LevelGridCluster(clusterConnectionPoints.get(clusterConnectionPoints.indexOf(point) - 1), point)));


        List<WalkerBuilder> walkers = initializeWalkers(clusters.values());

        //empty level grid will be filled with rooms during generation
        GridSection[][] grid = generateEmptyMapGrid(gridSize);
        val startSection = setStartSection(grid, clusterConnectionPoints.getFirst());
        val endSection = setEndSection(grid, clusterConnectionPoints.getLast());
        //TODO:consider removing after debug
        initConnectionSections(grid, clusterConnectionPoints);

        while (!walkers.isEmpty()) {
            for (WalkerBuilder walker : walkers) {
                walker.nextStep(grid);
                log.debug("Current grid state\n{}", printMapGridToLogs(grid));
            }
            walkers.removeIf(WalkerBuilder::isStopped);
        }

        log.debug("Clusters data: {}", clusters);
        log.debug("Current grid state\n{}", printMapGridToLogs(grid));

        processNegativeSectionsAndDeadEnds(clusters.values(), grid);

        Set<String> usedItemIds = new HashSet<>();
        Map<Point, Room> roomsMap = new HashMap<>();
        double levelDensity = clusters.values().stream().mapToInt(LevelGridCluster::getSize).sum() / (double) (gridSize * gridSize);
        log.debug("Populating connection point rooms with content...");
        for (int i = 0; i < clusterConnectionPoints.size(); i++) {
            if (i == 0) {
                Room start = buildStartRoom(clusterConnectionPoints.get(i), chatId);
                level.setStart(start);
                roomsMap.put(start.getPoint(), start);
                log.debug("Current map state\n{}", printMapToLogs(grid, roomsMap));
                continue;
            }
            if (i == clusterConnectionPoints.size() - 1) {
                Room end = buildEndRoom(clusterConnectionPoints.get(i), chatId);
                level.setEnd(end);
                roomsMap.put(end.getPoint(), end);
                Weight expectedWeight = weightCalculationService.getExpectedClusterConnectionPointWeight(player.getWeight(),
                        player.getWeight().toVector().getNorm() / levelDensity,
                        i, clusterConnectionPoints.size());
                clusters.get(clusterConnectionPoints.get(i)).setClusterExpectedWeight(expectedWeight);
                continue;
            }
            var currentSection = grid[clusterConnectionPoints.get(i).getX()][clusterConnectionPoints.get(i).getY()];
            log.debug("Current section:{}", currentSection);
            Weight expectedWeight = weightCalculationService.getExpectedClusterConnectionPointWeight(player.getWeight(),
                    player.getWeight().toVector().getNorm() / levelDensity,
                    i, clusterConnectionPoints.size());
            log.debug("Expected weight norm: {}, expected weight: {}", expectedWeight.toVector().getNorm(), expectedWeight);
            val roomType = getRandomClusterConnectionRoomType(expectedWeight);
            val roomContent = roomContentGenerationService.getNextRoomContent(expectedWeight, chatId, roomType, usedItemIds);
            if (roomContent instanceof ItemsRoom itemsRoom) {
                usedItemIds.addAll(itemsRoom.getItems().stream().map(Item::getId).collect(Collectors.toSet()));
            }
            clusters.get(currentSection.getPoint()).setClusterExpectedWeight(roomContent.getRoomContentWeight());
            Room room = buildRoom(currentSection, chatId, roomContent);
            roomsMap.put(room.getPoint(), room);
            log.debug("Current map state\n{}", printMapToLogs(grid, roomsMap));
        }

        for (LevelGridCluster cluster : clusters.values()) {
            Point endPoint = cluster.getEndConnectionPoint();
            val endAdjacentSectionsStream = getAdjacentSectionsInCluster(endPoint, grid, cluster).stream()
                    .filter(section -> section.getStepsFromStart() > 0);
            log.debug("Processing endSection: {}", endSection);
            int deadEndsRouteStepsCount = 0;
            if (cluster.hasDeadEnds()) {
                deadEndsRouteStepsCount = populateDeadEnds(chatId, player, grid, cluster, usedItemIds);
            }
            int mainPathLength = endAdjacentSectionsStream.max(Comparator.comparing(GridSection::getStepsFromStart))
                    .map(GridSection::getStepsFromStart).orElse(0);
            val mainPathWalker = WalkerDistributor.builder()
                    .chatId(chatId)
                    .cluster(cluster)
                    .currentSection(endSection)
                    .runSubWalkerOnRouteFork(true)
                    .previousRoom(level.getEnd())
                    .currentStep(mainPathLength)
                    .status(WalkerDistributor.Status.RUNNING)
                    .mainPathLength(mainPathLength)
                    .totalSteps(cluster.getSize() - deadEndsRouteStepsCount)
                    .build();
            while (!WalkerDistributor.Status.FINISHED.equals(mainPathWalker.getStatus())) {
                if (!WalkerDistributor.Status.WAITING.equals(mainPathWalker.getStatus())) {
                    log.debug("Main path walker next step...");
                    val nextRoom = mainPathWalker.nextStep(grid);
                    if (nonNull(nextRoom)) {
                        populateRoom(chatId, player, grid, usedItemIds, roomsMap, cluster, nextRoom);
                    }
                }
                log.debug("Removing finished sub-walkers");
                mainPathWalker.getSubWalkers().removeIf(walker -> WalkerDistributor.Status.FINISHED.equals(walker.getStatus()));
                log.debug("Processing running sub-walkers...");
                for (WalkerDistributor walker : mainPathWalker.getSubWalkers()) {
                    if (!WalkerDistributor.Status.WAITING.equals(walker.getStatus())) {
                        val nextRoom = walker.nextStep(grid);
                        if (nonNull(nextRoom)) {
                            populateRoom(chatId, player, grid, usedItemIds, roomsMap, cluster, nextRoom);
                        }
                    }
                }

            }
        }
        level.setRoomsMap(roomsMap);
        level.setGrid(grid);
        log.debug("Current map state\n{}", printMapToLogs(grid, roomsMap));
        return level;
    }

    private void populateRoom(long chatId, Player player,
                              GridSection[][] grid, Set<String> usedItemIds,
                              Map<Point, Room> roomsMap, LevelGridCluster cluster,
                              NextRoomDto nextRoom) {
        log.debug("Populating room...");
        Weight expectedWeight = weightCalculationService.getExpectedWeigth(cluster.getClusterExpectedWeight(),
                player.getWeight().toVector().getNorm() / cluster.getDensity(),
                nextRoom.getCurrentStep(),
                nextRoom.getTotalSteps());
        log.debug("Expected weight: {}", expectedWeight);
        val roomType = getRandomRoomType(expectedWeight, nextRoom.getCurrentStep(), nextRoom.getTotalSteps());
        log.debug("Room type: {}", roomType);
        RoomContent roomContent = roomContentGenerationService.getNextRoomContent(expectedWeight, chatId, roomType, usedItemIds);
        if (roomContent instanceof ItemsRoom itemsRoom) {
            usedItemIds.addAll(itemsRoom.getItems().stream().map(Item::getId).collect(Collectors.toSet()));
        }
        Room room = nextRoom.getRoom();
        setRoomContent(grid, room, roomContent);
        roomService.saveOrUpdateRoom(room);
        log.debug("Current map state\n{}", printMapToLogs(grid, roomsMap));
    }

    private int populateDeadEnds(long chatId, Player player, GridSection[][] grid, LevelGridCluster cluster, Set<String> usedItemIds) {
        return cluster.getDeadEnds().stream().mapToInt(section -> populateDeadEnd(chatId, player, grid, section, cluster, usedItemIds)).sum();
    }

    private int populateDeadEnd(long chatId, Player player, GridSection[][] grid, GridSection start, LevelGridCluster cluster, Set<String> usedItemIds) {
        val roomContent = roomContentGenerationService.getSpecialTreasure(chatId, player.getAttributes().get(PlayerAttribute.LUCK), usedItemIds);
        usedItemIds.addAll(roomContent.getItems().stream().map(Item::getId).collect(Collectors.toSet()));
        val rewardWeight = roomContent.getRoomContentWeight();
        val room = buildRoom(start, chatId, roomContent);
        val adjacentSections = getAdjacentSectionsInCluster(start.getPoint(), grid, cluster).stream()
                .filter(section -> section.getStepsFromStart() == start.getStepsFromStart() - 1)
                .toList();
        ArrayList<WalkerDistributor> deadEndPopulatingWalkers;
        int totalRooms = 1;
        Weight lastAddedWeight = null;
        if (adjacentSections.size() > 1) {
            deadEndPopulatingWalkers = Stream.of(WalkerDistributor.builder()
                            .chatId(chatId)
                            .previousRoom(room)
                            .runSubWalkerOnRouteFork(false)
                            .status(WalkerDistributor.Status.RUNNING)
                            .cluster(cluster)
                            .totalSteps(totalRooms)
                            .currentSection(adjacentSections.getFirst())
                            .currentStep(start.getStepsFromStart())
                            .build(),
                    WalkerDistributor.builder()
                            .chatId(chatId)
                            .previousRoom(room)
                            .runSubWalkerOnRouteFork(false)
                            .status(WalkerDistributor.Status.RUNNING)
                            .cluster(cluster)
                            .totalSteps(totalRooms)
                            .currentSection(adjacentSections.getLast())
                            .currentStep(start.getStepsFromStart())
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
                    .currentSection(adjacentSections.getFirst())
                    .currentStep(start.getStepsFromStart())
                    .build()).collect(Collectors.toCollection(ArrayList::new));
        }
        while (!deadEndPopulatingWalkers.isEmpty()) {
            totalRooms += deadEndPopulatingWalkers.stream().mapToInt(WalkerDistributor::getTotalSteps).sum();
            deadEndPopulatingWalkers.removeIf(walkerDistributor -> WalkerDistributor.Status.FINISHED.equals(walkerDistributor.getStatus()));
            for (WalkerDistributor walkerDistributor : deadEndPopulatingWalkers) {
                val next = walkerDistributor.nextStep(grid);
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
                    setRoomContent(grid, nextRoom, nextRoomContent);
                    roomService.saveOrUpdateRoom(room);
                }
            }
        }
        return totalRooms;
    }

    private void initConnectionSections(GridSection[][] grid, LinkedList<Point> clusterConnectionPoints) {
        clusterConnectionPoints.forEach(point -> {
            val section = new GridSection(point.getX(), point.getY());
            section.setConnectionPoint(true);
            grid[point.getX()][point.getY()] = section;
        });
    }

    private void processNegativeSectionsAndDeadEnds(Collection<LevelGridCluster> clusters, GridSection[][] grid) {
        log.debug("Processing negative sections and dead ends...");
        clusters.stream().filter(LevelGridCluster::hasNegativeRooms)
                .forEach(cluster -> {
                    log.debug("Processing negative rooms of cluster {}", cluster);
                    GridSection endSection = grid[cluster.getEndConnectionPoint().getX()][cluster.getEndConnectionPoint().getY()];
                    processNegativeSections(grid, cluster, endSection);
                });
        clusters.stream().filter(LevelGridCluster::hasDeadEnds)
                .forEach(cluster -> processDeadEnds(grid, cluster));
    }

    private void processDeadEnds(GridSection[][] grid, LevelGridCluster cluster) {
        log.debug("Processing deadEnds of cluster {}", cluster);
        List<GridSection> processedDeadEnds = new ArrayList<>();
        cluster.getDeadEnds().removeIf(deadEnd -> {
            val adjacentSections = getAdjacentSectionsInCluster(deadEnd.getPoint(), grid, cluster);
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
                            for (GridSection section : getAdjacentSectionsInCluster(currentSection.getPoint(), grid, cluster)) {
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
        log.debug("Processed dead ends:{}", processedDeadEnds);
        processedDeadEnds.removeIf(deadEnd -> {
            val adjacentSections = getAdjacentSectionsInCluster(deadEnd.getPoint(), grid, cluster);
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
        log.debug("Processed dead ends filtered out: {}", processedDeadEnds);
        cluster.addDeadEnds(processedDeadEnds);
        log.debug("Cluster data:{}", cluster);
        log.debug("Current grid state\n{}", printMapGridToLogs(grid));
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
        log.debug("Processing section {} of cluster {}", currentSection, cluster);
        val adjacentSections = getAdjacentSectionsInCluster(currentSection.getPoint(), grid, cluster);
        var stepsFromStart = currentSection.getStepsFromStart();
        val negativeAdjacentSections = adjacentSections.stream()
                .filter(section -> section.getStepsFromStart() < 0)
                .collect(Collectors.toSet());
        if (negativeAdjacentSections.size() > 0) {
            negativeAdjacentSections
                    .forEach(section -> processNegativeBranch(section, stepsFromStart, grid, cluster));
        }
        if (adjacentSections.stream().filter(section -> section.getStepsFromStart() > 0).count() < 3) {
            adjacentSections.stream()
                    .filter(section -> section.getStepsFromStart() > 0)
                    .filter(section -> !currentSection.getPoint().equals(section.getPoint()))
                    .findFirst()
                    .ifPresent(section ->
                            processNegativeSections(grid, cluster, currentSection));
        }
        log.debug("Current grid state\n{}", printMapGridToLogs(grid));
    }

    private void processNegativeBranch(GridSection section, int stepsFromStart, GridSection[][] grid, LevelGridCluster cluster) {
        log.debug("Processing negative branch of section {}, steps from start: {}", section, stepsFromStart);
        Optional<GridSection> currentSection = Optional.of(section);
        Optional<GridSection> lastSection = Optional.empty();
        int counter = 0;
        while (currentSection.isPresent()) {
            lastSection = currentSection;
            Optional<GridSection> nextSection = Optional.empty();
            for (GridSection adjacentSection : getAdjacentSectionsInCluster(currentSection.get().getPoint(), grid, cluster)) {
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
            currentSection.get().setStepsFromStart(counter);
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

    private List<WalkerBuilder> initializeWalkers(Collection<LevelGridCluster> clusters) {
        log.debug("Initializing walkers for clusters: {}", clusters);
        return clusters.stream().flatMap(cluster -> {
            log.debug("Processing cluster: {}", cluster);
            if (cluster.isSmallCluster()) {
                log.debug("Small cluster, adding two border walkers to start cluster...");
                return Stream.of(WalkerBuilder.builder()
                                .pathFromStart(0)
                                .isReversed(false)
                                .cluster(cluster)
                                .longestPathDefault(true)
                                .currentPoint(cluster.getStartConnectionPoint())
                                .build(),
                        WalkerBuilder.builder()
                                .pathFromStart(0)
                                .isReversed(false)
                                .longestPathDefault(true)
                                .cluster(cluster)
                                .currentPoint(cluster.getStartConnectionPoint())
                                .build());
            } else if (cluster.hasSmallSide()) {
                log.debug("Small sided cluster...");
                return Stream.of(WalkerBuilder.builder()
                                .pathFromStart(0)
                                .isReversed(false)
                                .cluster(cluster)
                                .longestPathDefault(false)
                                .currentPoint(cluster.getStartConnectionPoint())
                                .build(),
                        WalkerBuilder.builder()
                                .isReversed(true)
                                .longestPathDefault(true)
                                .pathFromStart(0)
                                .cluster(cluster)
                                .currentPoint(cluster.getEndConnectionPoint())
                                .build());
            }
            int fromStartWalkersNumber = getRandomInt(1, 2);
            int fromEndWalkersNumber = getRandomInt(1, 3 - fromStartWalkersNumber);

            log.debug("Adding {} walkers to start of cluster, {} walkers to end of cluster",
                    fromStartWalkersNumber, fromEndWalkersNumber);
            return IntStream.range(0, fromStartWalkersNumber + fromEndWalkersNumber).mapToObj(i -> {
                if (i < fromStartWalkersNumber) {
                    return WalkerBuilder.builder()
                            .pathFromStart(0)
                            .isReversed(false)
                            .longestPathDefault(fromStartWalkersNumber == 2 && i == 0)
                            .cluster(cluster)
                            .currentPoint(cluster.getStartConnectionPoint())
                            .build();
                } else {
                    return WalkerBuilder.builder()
                            .isReversed(true)
                            .pathFromStart(0)
                            .longestPathDefault(fromEndWalkersNumber == 1 || i == 2)
                            .cluster(cluster)
                            .currentPoint(cluster.getEndConnectionPoint())
                            .build();
                }
            });
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private LinkedList<Point> generateClusterConnectionPoints(Point start, Point end) {
        log.debug("Generating cluster connection points...");
        LinkedList<Point> clusterConnectingPoints = new LinkedList<>();
        clusterConnectingPoints.add(start);
        clusterConnectingPoints.add(end);

        Map<Integer, Point> points;
        do {
            points = new HashMap<>();
            for (int i = 0; i < clusterConnectingPoints.size() - 1; i++) {
                val nextPoint = getClustersConnectionPoint(clusterConnectingPoints.get(i), clusterConnectingPoints.get(i + 1));
                if (nextPoint.isPresent()) {
                    log.debug("Adding point {} to position {}", nextPoint.get(), i + 1);
                    points.put(i + 1, nextPoint.get());
                }
            }
            log.debug("Points to add: {}", points);
            points.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())).forEach(entry -> {
                        clusterConnectingPoints.add(entry.getKey(), entry.getValue());
                        log.debug("Point {} added to position {}", entry.getValue(), entry.getKey());
                    }
            );
        } while (!points.isEmpty());
        return clusterConnectingPoints;
    }

    private Optional<Point> getClustersConnectionPoint(Point start, Point end) {
        log.debug("Calculating connection point for start: {}, end: {}", start, end);
        if ((abs(start.getX() - end.getX()) > 4) && (abs(start.getY() - end.getY()) > 4)) {
            int x = getNextConnectionPointCoord(start.getX(), end.getX());
            log.debug("x coordinate of next connection point: {}", x);
            int y = getNextConnectionPointCoord(start.getY(), end.getY());
            log.debug("y coordinate of next connection point: {}", y);
            return Optional.of(new Point(x, y));
        }
        log.debug("Cluster is too small to divide, no point added");
        return Optional.empty();
    }

    private int getNextConnectionPointCoord(int start, int end) {
        int center = (end - start) / 2 + start;
        log.debug("Center: {}", center);
        int rangeStart = center - 1;
        int rangeEnd;
        if (abs(end - start) % 2 == 0) {
            rangeEnd = center + 1;
        } else {
            rangeEnd = center + 2;
        }
        log.debug("Generating random number between {} and {}", rangeStart, rangeEnd);
        return getRandomInt(rangeStart, rangeEnd);
    }

    private void setRoomContent(GridSection[][] grid, Room room, RoomContent roomContent) {
        log.debug("Setting type {} to room [x:{}, y:{}]", roomContent.getRoomType(), room.getPoint().getX(), room.getPoint().getY());
        grid[room.getPoint().getX()][room.getPoint().getY()].setEmoji(getIcon(Optional.of(roomContent.getRoomType())));
        room.setRoomContent(roomContent);
        roomService.saveOrUpdateRoom(room);
        log.debug("Current grid state\n{}", printMapGridToLogs(grid));
    }

    private Room buildRoom(GridSection section, Long chatId, RoomContent roomContent) {
        val room = new Room();
        room.setChatId(chatId);
        section.setEmoji(getIcon(Optional.ofNullable(roomContent.getRoomType())));
        room.setPoint(section.getPoint());
        room.setRoomContent(roomContent);
        return roomService.saveOrUpdateRoom(room);
    }

    private Room buildStartRoom(Point point, Long chatId) {
        return roomService.saveOrUpdateRoom(new Room(point, chatId, new StartRoom()));
    }

    private Room buildEndRoom(Point point, Long chatId) {
        return roomService.saveOrUpdateRoom(new Room(point, chatId, new EndRoom()));
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

    private GridSection setEndSection(GridSection[][] grid, Point endPoint) {
        val endSection = grid[endPoint.getX()][endPoint.getY()];
        endSection.setStepsFromStart(0);
        endSection.setEmoji(getIcon(Optional.of(RoomType.END)));
        return endSection;
    }
}
