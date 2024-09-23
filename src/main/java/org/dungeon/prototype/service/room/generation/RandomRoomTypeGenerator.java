package org.dungeon.prototype.service.room.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.inventory.Item;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.room.content.ItemsRoom;
import org.dungeon.prototype.model.room.content.NormalRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.weight.Weight;
import org.dungeon.prototype.service.room.RoomService;
import org.dungeon.prototype.service.room.generation.room.content.RoomContentGenerationService;
import org.dungeon.prototype.util.LevelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import static org.dungeon.prototype.util.GenerationUtil.getWeightLimitNormalization;

@Slf4j
@Component
public class RandomRoomTypeGenerator {
    @Autowired
    private RoomContentGenerationService roomContentGenerationService;
    @Autowired
    private RoomService roomService;

    /**
     * Generates clusters of content to distribute to given level
     * @param level to add content to
     * @param player current player
     * @return level room content cluster container
     */
    public LevelRoomTypeClusters generateClusters(Level level, Player player) {
        log.debug("Initializing room type generator parameters...");
        val levelRoomTypeClusters = new LevelRoomTypeClusters();
        levelRoomTypeClusters.setRoomsLeft(level.getRoomsMap().size() - 2);
        levelRoomTypeClusters.setChatId(level.getChatId());
        levelRoomTypeClusters.setTotalRooms(levelRoomTypeClusters.getRoomsLeft());
        levelRoomTypeClusters.setExpectedWeight(player.getWeight() //TODO: test and adjust formula
                .multiply(levelRoomTypeClusters.getTotalRooms().doubleValue()));
        levelRoomTypeClusters.setDeadEnds(new PriorityQueue<>(Comparator.comparing(GridSection::getStepsFromStart)));
        level.getDeadEnds().forEach(levelRoomTypeClusters.getDeadEnds()::offer);
        levelRoomTypeClusters.setDeadEndToSegmentMap(level.getDeadEndToSegmentMap());
        levelRoomTypeClusters.setMainSegment(LevelUtil.getMainSegment(level));
        log.debug("Parameters - levelNumber:{}, deadEnds:{},  total: {}",
                level.getNumber(), levelRoomTypeClusters.getDeadEnds().size(),
                levelRoomTypeClusters.getRoomsLeft());
        log.debug("Start generating clusters...");
        Map<RoomsSegment, RoomTypesCluster> clusters = new HashMap<>();
        var expectedWeight = levelRoomTypeClusters.getExpectedWeight();
        while (levelRoomTypeClusters.getRoomsLeft() > 0) {
            val deadEnds = levelRoomTypeClusters.getDeadEnds();
            log.debug("deadEnds: {}", deadEnds);
            log.debug("deadEndToSegmentMap: {}", levelRoomTypeClusters.getDeadEndToSegmentMap());
            val noDeadEndLeft = deadEnds.isEmpty();
            if (noDeadEndLeft) {
                log.debug("Out of dead ends! Proceeding to end room...");
            }
            GridSection deadEnd;
            RoomsSegment segment;
            int clusterSize;
            if (noDeadEndLeft) {
                deadEnd = levelRoomTypeClusters.getMainSegment().getEnd();
                segment = levelRoomTypeClusters.getMainSegment();
                clusterSize = deadEnd.getStepsFromStart() - 1;
            } else {
                deadEnd = deadEnds.poll();
                segment = levelRoomTypeClusters.getDeadEndToSegmentMap().get(deadEnd.getPoint());
                clusterSize = segment.getEnd().getStepsFromStart() - segment.getStart().getStepsFromStart();
            }
            log.debug("Current room: {}, current segment: {}", deadEnd.getPoint(), segment);
            log.debug("Generating cluster with expected weight: {}", expectedWeight);
            val nextCluster = generateCluster(clusterSize, expectedWeight, levelRoomTypeClusters);
            expectedWeight = nextCluster.getClusterWeight().getNegative().add(clusters.values().stream().map(RoomTypesCluster::getClusterWeight).reduce(Weight::add).orElse(new Weight()));
            expectedWeight = getWeightLimitNormalization(expectedWeight, levelRoomTypeClusters.getExpectedWeight().toVector().getNorm(),
                            levelRoomTypeClusters.getTotalRooms() - levelRoomTypeClusters.getRoomsLeft(),
                            levelRoomTypeClusters.getTotalRooms());
            clusters.put(segment, nextCluster);
            if (deadEnds.isEmpty() && levelRoomTypeClusters.getRoomsLeft() > 0) {
                log.warn("WARNING! Undistributed rooms left: {}!", levelRoomTypeClusters.getRoomsLeft());
            }
        }
        log.debug("Generated clusters: {}", clusters);
        levelRoomTypeClusters.setClusters(clusters);
        return levelRoomTypeClusters;
    }

    private RoomTypesCluster generateCluster(int totalClusterRooms, Weight expectedWeight, LevelRoomTypeClusters levelRoomTypeClusters) {
        log.debug("Generating cluster size {}...", totalClusterRooms);
        var cluster = new RoomTypesCluster(totalClusterRooms);
        RoomContent roomContent;
        while (cluster.hasRoomLeft()) {
            var clusterWeight = cluster.getClusterWeight();
            log.debug("Next expected weight absolute value: {}", expectedWeight);
            log.debug("Generating random room...");
            var clusterRoomsLeft = cluster.getRoomsLeft();
            val currentStep = totalClusterRooms - clusterRoomsLeft;//TODO verify with debugger
            roomContent = nextRoomContent(expectedWeight, levelRoomTypeClusters);
            var lastAddedWeight = cluster.addRoom(roomContent);
            log.debug("Last added weight: {}", lastAddedWeight);
            log.debug("Added room content, {}", roomContent);
            expectedWeight = clusterWeight.add(lastAddedWeight.getNegative());
            expectedWeight = getWeightLimitNormalization(expectedWeight, levelRoomTypeClusters.getExpectedWeight().toVector().getNorm(), currentStep, totalClusterRooms);
            log.debug("Generated cluster: {}", cluster);
        }
        return cluster;
    }

    private RoomContent nextRoomContent(Weight expectedWeight, LevelRoomTypeClusters levelRoomTypeClusters) {
        if (expectedWeight.toVector().getNorm() == 0.0) {//TODO: consider configuring threshold
            return new NormalRoom();
        }
        val roomContent = roomContentGenerationService.getNextRoomContent(levelRoomTypeClusters, expectedWeight);
        if (roomContent instanceof ItemsRoom itemsRoom) {
            Set<Item> items = itemsRoom.getItems();
            levelRoomTypeClusters.addUsedItemsIds(items.stream().map(Item::getId).collect(Collectors.toSet()));
        }
        levelRoomTypeClusters.decreaseRoomsCount();
        return roomService.saveOrUpdateRoomContent(roomContent);
    }

    public void updateDeadEndsForDistribution(Level level, LevelRoomTypeClusters clusters) {
        clusters.getDeadEndToSegmentMap().keySet().stream()
                .map(point -> level.getGrid()[point.getX()][point.getY()])
                .forEach(clusters.getDeadEnds()::offer);
    }

}
