package org.dungeon.prototype.service.room;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.util.LevelUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.math3.util.FastMath.abs;
import static org.dungeon.prototype.model.room.RoomType.HEALTH_SHRINE;
import static org.dungeon.prototype.model.room.RoomType.MANA_SHRINE;
import static org.dungeon.prototype.model.room.RoomType.MERCHANT;
import static org.dungeon.prototype.model.room.RoomType.MONSTER;
import static org.dungeon.prototype.model.room.RoomType.NORMAL;
import static org.dungeon.prototype.model.room.RoomType.TREASURE;
import static org.dungeon.prototype.service.room.RoomContentRandomFactory.getNextRoomContent;
import static org.dungeon.prototype.util.RandomUtil.getNextRoomType;

@Slf4j
public class RandomRoomTypeGenerator {
    private static Integer totalRooms;
    private final Map<RoomsSegment, RoomTypesCluster> clusters = new HashMap<>();
    private final PriorityQueue<GridSection> deadEnds;
    private final Map<Point, RoomsSegment> deadEndToSegmentMap;
    @Getter
    private final RoomsSegment mainSegment;
    @Getter
    private Integer roomMonsters;
    @Getter
    private Integer roomTreasures;
    private Integer roomsLeft;
    private boolean hasHealthShrineRoom;
    private boolean hasManaShrineRoom;
    private boolean hasMerchantRoom;

    public RandomRoomTypeGenerator(Level level) {
        log.debug("Initializing room type generator...");
        this.roomsLeft = level.getRoomsMap().size() - 2;
        totalRooms = roomsLeft;
        roomMonsters = 0;
        roomTreasures = 0;
        hasHealthShrineRoom = false;
        hasManaShrineRoom = false;
        hasMerchantRoom = false;
        this.deadEnds = new PriorityQueue<>(Comparator.comparing(GridSection::getStepsFromStart));
        level.getDeadEnds().forEach(this.deadEnds::offer);
        this.deadEndToSegmentMap = level.getDeadEndToSegmentMap();
        this.mainSegment = LevelUtil.getMainSegment(level);
        log.debug("Parameters - levelNumber:{}, deadEnds:{}, roomMonsters: {}, roomTreasures: {},  total: {}", level.getNumber(), deadEnds.size(), roomMonsters, roomTreasures, roomsLeft);
        generateClusters();
    }

    public boolean hasDeadEnds() {
        return !deadEnds.isEmpty();
    }

    public GridSection getNextDeadEnd() {
        return deadEnds.poll();
    }
    public boolean hasClusters() {
        return !clusters.isEmpty();
    }


    public RoomTypesCluster getClusterBySegment(RoomsSegment segment) {
        val cluster = clusters.get(segment);
        clusters.remove(segment);
        return cluster;
    }

    public RoomsSegment getSegmentByDeadEnd(Point point) {
        return deadEndToSegmentMap.get(point);
    }

    public void generateClusters() {
        log.debug("Start generating clusters...");
        var expectedRange = 0;
        var expectedWeightAbs = 0;
        while (roomsLeft > 0) {
            log.debug("deadEnds: {}", deadEnds);
            log.debug("deadEndToSegmentMap: {}", deadEndToSegmentMap);
            val noDeadEndLeft = deadEnds.isEmpty();
            if (noDeadEndLeft) {
                log.debug("Out of dead ends! Proceeding to end room...");
            }
            GridSection deadEnd;
            RoomsSegment segment;
            int clusterSize;
            if (noDeadEndLeft) {
                deadEnd = this.mainSegment.getEnd();
                segment = this.mainSegment;
                clusterSize = deadEnd.getStepsFromStart() - 1;
            } else {
                deadEnd = deadEnds.poll();
                segment = deadEndToSegmentMap.get(deadEnd.getPoint());
                clusterSize = segment.getEnd().getStepsFromStart() - segment.getStart().getStepsFromStart();
            }
            log.debug("Current room: {}, current segment: {}", deadEnd.getPoint(), segment);
            val nextCluster = generateCluster(clusterSize, expectedRange, expectedWeightAbs);
            expectedRange = nextCluster.getMiddleAbsWeight();
            expectedWeightAbs = abs(nextCluster.getLastAddedWeight());
            log.debug("Current expected weight range: {} , expected weight (abs): {}", expectedRange, expectedWeightAbs);
            clusters.put(segment, nextCluster);
            roomsLeft -= clusterSize;
            if (deadEnds.isEmpty() && roomsLeft > 0) {
                log.warn("WARNING! Undistributed rooms left: {}!", roomsLeft);
            }
        }
        log.debug("Generated clusters: {}", clusters);
    }

    public RoomTypesCluster generateCluster(int totalRooms, int expectedRange, Integer expectedWeightAbs) {
        log.debug("Generating cluster size {}...", totalRooms);
        log.debug("Current expected weight range: {}", expectedRange);
        var cluster = new RoomTypesCluster(totalRooms);
        var lastAddedWeight = 0;
        var middleWeight = 0;
        RoomContent roomContent;
        while (cluster.hasRoomLeft()) {
            log.debug("Next expected weight absolute value: {}", expectedWeightAbs);
            var exclude = getExcludedSpecialRoomTypes();
            log.debug("Room types to exclude: {}", exclude);
            val currentStep = totalRooms - cluster.getTotalRooms() + 1;
            if (abs(lastAddedWeight) < expectedRange) {
                log.debug("Generating random room...");
                roomContent = nextRoomContent(exclude, currentStep, expectedWeightAbs);
            } else {
                if (middleWeight <= 0) {
                    log.debug("Positive weight room generation...");
                    var excludeNegativeAndZero = new HashSet<>(exclude);
                    excludeNegativeAndZero.addAll(Set.of(MONSTER, NORMAL));
                    roomContent = nextRoomContent(excludeNegativeAndZero, currentStep, expectedWeightAbs);
                } else {
                    log.debug("Negative weight room generation...");
                    var excludePositiveAndZero = new HashSet<>(exclude);
                    excludePositiveAndZero.addAll(Set.of(HEALTH_SHRINE, MANA_SHRINE, MERCHANT, TREASURE, NORMAL));
                    roomContent = nextRoomContent(excludePositiveAndZero, currentStep, expectedWeightAbs);
                }
            }
            lastAddedWeight = cluster.addRoom(roomContent);
            middleWeight = cluster.getMiddleWeight();
            log.debug("Last added weight: {}", lastAddedWeight);
            log.debug("Added room content, {}", roomContent);
            expectedWeightAbs =  cluster.getMiddleAbsWeight();
        }
        log.debug("Generated cluster: {}", cluster);
        return cluster;
    }


    private Set<RoomType> getExcludedSpecialRoomTypes() {
        Set<RoomType> exclude = new HashSet<>();
        if (hasHealthShrineRoom) {
            exclude.add(RoomType.HEALTH_SHRINE);
            log.debug("Health shrine already on the level");
        }
        if (hasManaShrineRoom) {
            exclude.add(RoomType.MANA_SHRINE);
            log.debug("Mana shrine already on the level");
        }
        if (hasMerchantRoom) {
            exclude.add(RoomType.MERCHANT);
            log.debug("Merchant is already on the level");
        }
        return exclude;
    }

    public RoomContent nextRoomContent(Set<RoomType> exclude, int currentStep, Integer expectedWeightAbs) {
        log.debug("Generating next room type...");
        RoomType roomType;
        if (!exclude.isEmpty()) {
            log.debug("Excluded room types: {}", exclude);
            log.debug("Generating next room type...");
            val probabilities = getExcludeTypesProbabilities(exclude);
            roomType = getNextRoomType(roomsLeft, totalRooms, roomMonsters, roomTreasures, currentStep, probabilities);
            log.debug("Random room type: {}", roomType);
        } else {
            log.debug("Generating next room type...");
            roomType = getNextRoomType(roomsLeft, totalRooms, roomMonsters, roomTreasures, currentStep, Collections.emptyMap());
            log.debug("Random room type: {}", roomType);
        }
        log.debug("Selected room type: {}", roomType);
        adjustCountersAndFlags(roomType, expectedWeightAbs);

        return getNextRoomContent(roomType, expectedWeightAbs);
    }

    private Map<RoomType, Double> getExcludeTypesProbabilities(Set<RoomType> exclude) {
        return exclude.stream().collect(Collectors.toMap(Function.identity(), roomType -> 0.0));
    }

    private void adjustCountersAndFlags(RoomType roomType, Integer expectedWeightAbs) {
        switch (roomType) {
            case MONSTER -> {
                if (expectedWeightAbs > 0) roomMonsters++;
            }
            case TREASURE -> {
                if (expectedWeightAbs > 0) roomTreasures++;
            }
            case MERCHANT -> {
                if (expectedWeightAbs > 0) hasMerchantRoom = true;
            }
            case HEALTH_SHRINE -> hasHealthShrineRoom = true;
            case MANA_SHRINE -> hasManaShrineRoom = true;
        }
        roomsLeft--;
    }

    @Override
    public String toString() {
        return "deadEnds: " + deadEnds + ", total: " + deadEnds.size() + "\n" +
                "deadEndToSegmentMap" + deadEndToSegmentMap + ", total: " + deadEndToSegmentMap.size() + "\n" +
                "cluster" + clusters + ", total: " + clusters.size();
    }

    public void updateDeadEndsForDistribution(Level level) {
        deadEndToSegmentMap.keySet().stream()
                .map(point -> level.getGrid()[point.getX()][point.getY()])
                .forEach(deadEnds::offer);
    }
}
