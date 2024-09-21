package org.dungeon.prototype.service.room.generation;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.weight.Weight;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Data
@NoArgsConstructor
public class LevelRoomTypeClusters {
    private Long chatId;
    private Integer totalRooms;
    private Set<String> usedItemIds;
    private Map<RoomsSegment, RoomTypesCluster> clusters = new HashMap<>();
    private Weight expectedWeight;
    private PriorityQueue<GridSection> deadEnds;
    private Map<Point, RoomsSegment> deadEndToSegmentMap;
    @Getter
    private RoomsSegment mainSegment;
    @Getter
    private Integer roomTreasures;
    private Integer roomsLeft;
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
