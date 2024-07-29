package org.dungeon.prototype.service.room.generation;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.monster.MonsterClass;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.ui.level.GridSection;

import java.util.*;

@Data
@NoArgsConstructor
public class LevelRoomTypeClusters {
    private Long chatId;
    private Integer totalRooms;
    private Set<String> usedItemIds;
    private Map<RoomsSegment, RoomTypesCluster> clusters = new HashMap<>();
    private PriorityQueue<GridSection> deadEnds;
    private Map<Point, RoomsSegment> deadEndToSegmentMap;
    private Map<MonsterClass, Integer> monsterClassesCounters;
    @Getter
    private RoomsSegment mainSegment;
    @Getter
    private Integer roomTreasures;
    private Integer roomsLeft;
    private Double healthShrineInitialProbability;
    private Double manaShrineInitialProbability;
    private Double merchantInitialProbability;
    private boolean hasHealthShrineRoom;
    private boolean hasManaShrineRoom;
    private boolean hasMerchantRoom;

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

    public boolean addUsedItemsIds(Collection<String> itemIds) {
        return usedItemIds.addAll(itemIds);
    }

    public RoomsSegment getSegmentByDeadEnd(Point point) {
        return deadEndToSegmentMap.get(point);
    }
}
