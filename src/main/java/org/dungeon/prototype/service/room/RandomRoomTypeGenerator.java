package org.dungeon.prototype.service.room;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.room.Room;

import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfMonsters;
import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfTreasures;
import static org.dungeon.prototype.util.LevelUtil.getRoomTypeWeights;

@Slf4j
public class RandomRoomTypeGenerator {
    private final NavigableMap<Integer, Room.Type> map;
    private final Random random;
    private final int total;
    private Queue<RoomTypesCluster> clusters =
            new PriorityQueue<>(Comparator.comparing(RoomTypesCluster::getClusterWeight));
    @Getter
    private Integer roomMonsters;
    @Getter
    private Integer roomTreasures;
    private final Integer clusterWeightRange;
    private boolean hasShrineRoom;
    private boolean hasMerchantRoom;

    public RandomRoomTypeGenerator(Integer roomTotal) {
        log.debug("Initializing room type generator...");
        random = new Random();
        map = getRoomTypeWeights();
        roomMonsters = calculateAmountOfMonsters(roomTotal);
        roomTreasures = calculateAmountOfTreasures(roomTotal);
        hasShrineRoom = false;
        hasMerchantRoom = false;
        clusterWeightRange = 200; //todo investigate calculation depending on level
        total = map.keySet().stream().mapToInt(k -> k).sum();
        log.debug("Parameters - roomMonsters: {}, roomTreasures: {}, clusterWeightRange: {}, total: {}", roomMonsters, roomTreasures, clusterWeightRange, total);
        generateClusters(roomTotal);
    }

    public boolean hasClusters() {
        return !clusters.isEmpty();
    }

    public RoomTypesCluster getNextCluster() {
        return clusters.poll();
    }

    public void generateClusters(int totalRooms) {
        val minClusterSize = 5;
        val maxClusterSize = totalRooms / 5 > minClusterSize ? totalRooms / 5 : minClusterSize + 5;
        log.debug("Cluster size range: {} - {}", minClusterSize, maxClusterSize);
        while (totalRooms > maxClusterSize) {
            var clusterSize = random.nextInt(maxClusterSize - minClusterSize) + minClusterSize;
            clusters.offer(generateCluster(clusterSize));
            totalRooms -= clusterSize;
        }
        clusters.offer(generateCluster(totalRooms));
        log.debug("Generated clusters: {}", clusters);
    }

    public RoomTypesCluster generateCluster(int totalRooms) {
        log.debug("Generating cluster size {}...", totalRooms);
        var cluster = new RoomTypesCluster(totalRooms);
        var clusterWeight = cluster.getClusterWeight();
        val exclude = getExcludedRoomTypes();
        Room.Type roomType;
        while (cluster.hasRoomLeft()) {
            if (clusterWeight > -clusterWeightRange && clusterWeight < clusterWeightRange) {
                log.debug("Generating random room...");
                roomType = nextRoomType(exclude);
            } else {
                if (clusterWeight < 0) {
                    log.debug("Positive weight room generation...");
                    var excludeNegative = new HashSet<>(exclude);
                    excludeNegative.add(Room.Type.MONSTER);
                    roomType = nextRoomType(excludeNegative);
                } else {
                    log.debug("Negative weight room generation...");
                    var excludePositive = new HashSet<>(exclude);
                    excludePositive.addAll(Set.of(Room.Type.SHRINE, Room.Type.TREASURE, Room.Type.MERCHANT));
                    roomType = nextRoomType(excludePositive);
                }
            }
            clusterWeight = cluster.addRoom(roomType);
        }
        log.debug("Generated cluster: {}", cluster);
        return cluster;
    }



    private Set<Room.Type> getExcludedRoomTypes() {
        Set<Room.Type> exclude = new HashSet<>();
        if (roomMonsters == 0) {
            exclude.add(Room.Type.MONSTER);
            log.debug("No monster room left");
        }
        if (roomTreasures == 0) {
            exclude.add(Room.Type.TREASURE);
            log.debug("No treasures room left");
        }
        if (hasShrineRoom) {
            exclude.add(Room.Type.SHRINE);
            log.debug("Shrine already on the level");
        }
        if (hasMerchantRoom) {
            exclude.add(Room.Type.MERCHANT);
            log.debug("Merchant is already on the level");
        }
        return exclude;
    }

    public Room.Type nextRoomType(Set<Room.Type> exclude) {
        log.debug("Generating next room type...");
        Room.Type roomType = null;
        if (!exclude.isEmpty()) {
            log.debug("Excluded room types: {}", exclude);
            while (roomType == null || exclude.contains(roomType)) {
                int randomWeight = random.nextInt(map.lastKey());
                log.debug("Random weight: {}", randomWeight);
                if (map.ceilingEntry(randomWeight) != null) {
                    roomType = map.ceilingEntry(randomWeight).getValue();
                    log.debug("Random room type: {}", roomType);
                }
            }
        } else {
            int randomWeight = random.nextInt(map.lastKey());
            log.debug("Random weight: {}", randomWeight);
            while (roomType == null) {
                if (map.ceilingEntry(randomWeight) != null) {
                    roomType = map.ceilingEntry(randomWeight).getValue();
                    log.debug("Random room type: {}", roomType);
                }
            }
        }
        switch (roomType) {
            case MONSTER -> roomMonsters--;
            case TREASURE -> roomTreasures--;
            case MERCHANT -> hasMerchantRoom = true;
            case SHRINE -> hasShrineRoom = true;
        }
        return roomType;
    }
}
