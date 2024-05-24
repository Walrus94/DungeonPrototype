package org.dungeon.prototype.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.util.LevelUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Getter
@Setter
@Builder(toBuilder = true)
public class Room {


    public enum Type {
        NORMAL, START, END, MONSTER, TREASURE, MONSTER_KILLED, TREASURE_LOOTED
    }
    @Builder.Default
    private Map<LevelUtil.Direction, Optional<Room>> adjacentRooms = getDefaultAdjacentRooms();

    private static Map<LevelUtil.Direction, Optional<Room>> getDefaultAdjacentRooms() {
        HashMap<LevelUtil.Direction, Optional<Room>> defaultMap = new HashMap<>();
        defaultMap.put(LevelUtil.Direction.N, Optional.empty());
        defaultMap.put(LevelUtil.Direction.W, Optional.empty());
        defaultMap.put(LevelUtil.Direction.E, Optional.empty());
        defaultMap.put(LevelUtil.Direction.S, Optional.empty());
        return defaultMap;
    }

    @Builder.Default
    private Type type = Type.NORMAL;
    @Builder.Default
    private boolean visitedByPlayer = false;
    private Point point;
    public void addAdjacentRoom(LevelUtil.Direction direction, Room nextRoom) {
        adjacentRooms.put(direction, Optional.of(nextRoom));
        log.debug("Added adjacent room {} to {} in {} direction", nextRoom.getPoint(), point, direction);
    }

    @Override
    public String toString() {
        return "Room [type=" + type + ", point=" + point + "]";
    }
}
