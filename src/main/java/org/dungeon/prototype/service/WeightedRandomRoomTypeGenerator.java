package org.dungeon.prototype.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;

import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfMonsters;
import static org.dungeon.prototype.util.LevelUtil.calculateAmountOfTreasures;
import static org.dungeon.prototype.util.LevelUtil.getRoomTypeWeights;

@Slf4j
public class WeightedRandomRoomTypeGenerator {
    private final NavigableMap<Double, Room.Type> map;

    private final Random random;
    private double total;
    @Getter
    private Integer roomMonsters;
    @Getter
    private Integer roomTreasures;

    public WeightedRandomRoomTypeGenerator(Integer roomTotal) {
        random = new Random();
        map = getRoomTypeWeights();
        roomMonsters = calculateAmountOfMonsters(roomTotal);
        roomTreasures = calculateAmountOfTreasures(roomTotal);
        total = map.keySet().stream().mapToDouble(Double::doubleValue).sum();
    }

    public Room.Type nextRoomType() {
        log.debug("Generating next room type...");
        List<Room.Type> exclude = new ArrayList<>();
        if (roomMonsters == 0) {
            exclude.add(Room.Type.MONSTER);
            log.debug("No monster room left");
        }
        if (roomTreasures == 0) {
            exclude.add(Room.Type.TREASURE);
            log.debug("No treasures room left");
        }
        Room.Type roomType;
        if (!exclude.isEmpty()) {
            roomType = exclude.getFirst();
            while (exclude.contains(roomType)) {
                double randomWeight = random.nextDouble() * total;
                log.debug("Random weight: {}", randomWeight);
                if (map.ceilingEntry(randomWeight) != null) {
                    roomType = map.ceilingEntry(randomWeight).getValue();
                } else {
                    roomType = Room.Type.NORMAL;
                }
            }
        } else {
            double randomWeight = random.nextDouble() * total;
            log.debug("Random weight: {}", randomWeight);
            if (map.ceilingEntry(randomWeight) != null) {
                roomType = map.ceilingEntry(randomWeight).getValue();
            } else {
                roomType = Room.Type.NORMAL;
            }
        }
        switch (roomType) {
            case MONSTER -> roomMonsters--;
            case TREASURE -> roomTreasures--;
        }
        return roomType;
    }
}
