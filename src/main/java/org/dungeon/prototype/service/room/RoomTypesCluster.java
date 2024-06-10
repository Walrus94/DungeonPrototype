package org.dungeon.prototype.service.room;

import lombok.Data;
import org.dungeon.prototype.model.room.RoomContent;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import static org.apache.commons.math3.util.FastMath.abs;
import static org.dungeon.prototype.util.LevelUtil.getIcon;

@Data
public class RoomTypesCluster {
    private Queue<RoomContent> rooms = new LinkedList<>();
    private Integer totalRooms;

    public RoomTypesCluster(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }

    public Integer getClusterWeight() {
        return Objects.isNull(rooms) || rooms.isEmpty() ? 0 : rooms.stream()
                .mapToInt(RoomContent::getRoomContentWeight)
                .sum();
    }
    public Integer getLastAddedWeight() {
        return Objects.isNull(rooms) || rooms.isEmpty() ? 0 : rooms.peek().getRoomContentWeight();
    }

    public Integer getMiddleWeight() {
        return Objects.isNull(rooms) ? null : rooms.isEmpty() ? 0 : rooms.stream()
                .mapToInt(RoomContent::getRoomContentWeight)
                .sum() / rooms.size();
    }

    public Integer getMiddleAbsWeight() {
        return Objects.isNull(rooms) || rooms.isEmpty() ? 0 : rooms.stream()
                .mapToInt(roomContent -> abs(roomContent.getRoomContentWeight()))
                .sum() / rooms.size();
    }

    public Integer addRoom(RoomContent roomContent) {
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

    @Override
    public String toString() {
        return "RoomTypesCluster{" +
                "clusterWeight=" + getClusterWeight() + ",\n" +
                "rooms=" + rooms.stream().map(roomContent -> getIcon(Optional.ofNullable(roomContent.getRoomType()))).collect(Collectors.joining(" ", "[", "]")) +
                '}';
    }
}
