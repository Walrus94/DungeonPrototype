package org.dungeon.prototype.service.room;

import lombok.Data;
import org.dungeon.prototype.model.room.RoomContent;
import org.dungeon.prototype.model.room.RoomType;
import org.dungeon.prototype.model.room.content.Merchant;
import org.dungeon.prototype.model.room.content.Monster;
import org.dungeon.prototype.model.room.content.NormalRoom;
import org.dungeon.prototype.model.room.content.Shrine;
import org.dungeon.prototype.model.room.content.Treasure;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
public class RoomTypesCluster {
    Random random = new Random();
    private Queue<RoomContent> rooms = new LinkedList<>();
    private Integer totalRooms;

    public RoomTypesCluster(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }

    public Integer getClusterWeight() {
        if (rooms.isEmpty()) {
            return 0;
        }
        return rooms.stream()
                .mapToInt(RoomContent::getRoomContentWeight)
                .sum();
    }

    private void addRoom(RoomContent roomContent) {
        rooms.add(roomContent);
        totalRooms--;
    }

    public boolean hasRoomLeft() {
        return totalRooms > 0;
    }

    public boolean hasNextRoomToDistribute() {
        return !rooms.isEmpty();
    }

    public RoomContent getNextRoom() {
        return rooms.poll();
    }

    public Integer addRoom(RoomType roomType) {
        switch (roomType) {
            case MONSTER -> addRoom(new Monster(random.nextInt(4) + 1)); //TODO: adjust according to level depth
            case TREASURE -> addRoom(new Treasure((random.nextInt(4) + 1) * 100));
            case MERCHANT -> addRoom(new Merchant());
            case SHRINE -> addRoom(new Shrine());
            case NORMAL -> addRoom(new NormalRoom());
        }
        return getClusterWeight();
    }

    @Override
    public String toString() {
        return "RoomTypesCluster{" +
                "clusterWeight=" + getClusterWeight() + ",\n" +
                "rooms=" + rooms.stream().map(roomContent -> getIcon(Optional.ofNullable(roomContent.getRoomType()))).collect(Collectors.joining(" ", "[", "]")) +
                '}';
    }
}
