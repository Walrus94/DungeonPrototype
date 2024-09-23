package org.dungeon.prototype.service.room.generation;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.RoomsSegment;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.weight.Weight;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static org.dungeon.prototype.model.room.RoomType.*;

@Data
@NoArgsConstructor
public class LevelRoomTypeClusters {
    private Long chatId;
    private Integer totalRooms;
    private Set<String> usedItemIds = new HashSet<>();
    private Map<RoomsSegment, RoomTypesCluster> clusters = new HashMap<>();
    private Weight expectedWeight;
    private PriorityQueue<GridSection> deadEnds;
    private Map<Point, RoomsSegment> deadEndToSegmentMap;
    private RoomsSegment mainSegment;
    private Integer roomsLeft;
    private EnumMap<RoomType, Set<Integer>> roomTypePoints = new EnumMap<>(Map.of(
            MERCHANT, new HashSet<>(),
            HEALTH_SHRINE, new HashSet<>(),
            MANA_SHRINE, new HashSet<>(),
            ANVIL, new HashSet<>()
    ));

    public boolean hasDeadEnds() {
        return !deadEnds.isEmpty();
    }
    public GridSection getNextDeadEnd() {
        return deadEnds.poll();
    }
    public boolean hasClusters() {
        return !clusters.isEmpty();
    }

    public void decreaseRoomsCount() {
        roomsLeft--;
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

    public void addPenaltyPoint(RoomType roomType, int point) {
        roomTypePoints.get(roomType).add(point);
    }
}
