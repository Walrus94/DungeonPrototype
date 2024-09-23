package org.dungeon.prototype.service.room.generation;

import lombok.Data;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.model.weight.Weight;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
public class RoomTypesCluster {
    private Queue<RoomContent> rooms = new LinkedList<>();
    private Integer totalRooms;

    public RoomTypesCluster(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }

    public Weight getClusterWeight() {
        return Objects.isNull(rooms) || rooms.isEmpty() ? new Weight() : rooms.stream()
                .map(RoomContent::getRoomContentWeight)
                .reduce(Weight::add).orElse(new Weight());
    }

    public Weight addRoom(RoomContent roomContent) {
        if (rooms.add(roomContent)) {
            totalRooms--;
            return roomContent.getRoomContentWeight();
        }
        return null;
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

    public int getRoomsLeft() {
        return rooms.size();
    }

    @Override
    public String toString() {
        return "RoomTypesCluster{" +
                "clusterWeightAbs=" + getClusterWeight().toVector().getNorm() + ",\n" +
                "rooms=" + rooms.stream().map(roomContent -> getIcon(Optional.ofNullable(roomContent.getRoomType()))).collect(Collectors.joining(" ", "[", "]")) +
                '}';
    }
}
