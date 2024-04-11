package org.dungeon.prototype.service;

import lombok.val;
import org.dungeon.prototype.model.Room;

import java.util.*;

import static org.dungeon.prototype.util.LevelUtil.getRoomTypeWeights;

public class WeightedRandomRoomTypeGenerator {
    private final NavigableMap<Double, Room.Type> map;

    private final Random random;
    private double total;

    public WeightedRandomRoomTypeGenerator() {
        random = new Random();
        map = getRoomTypeWeights();//TODO configure different level weights
        total = map.keySet().stream().mapToDouble(Double::doubleValue).sum();
    }

    public void add(Room.Type type, double weight) {
        map.put(total, type);
        total += weight;
    }

    public Room.Type nextRoomType(List<Room.Type> exclude) {
        if (exclude != null && !exclude.isEmpty()) {
            var roomType = exclude.getFirst();
            while (exclude.contains(roomType)) {
                double randomWeight = random.nextDouble() * total;
                if (map.ceilingEntry(randomWeight) != null) {
                    roomType = map.ceilingEntry(randomWeight).getValue();
                }
            }
            return roomType;
        } else {
            double randomWeight = random.nextDouble() * total;
            if (map.ceilingEntry(randomWeight) != null) {
                return map.ceilingEntry(randomWeight).getValue();
            } else {
                return Room.Type.NORMAL;
            }
        }
    }
}
